package net.rubenmartinez.stpc.exchange.bitso.trade.helper;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A circular buffer that accepts elements on both sides
 * 
 * Note that the maximum capacity of this buffer is not strictly enforced to improves efficiency.
 * So this class can be used to prevent the buffer growing too much but it is not ensured that at all moments the
 * size of the buffer will be less than the max capacity specified.
 */
public class ConcurrentSoftLimitedBuffer<T> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentSoftLimitedBuffer.class);
	
	private static final int DEFAULT_CAPACITY_CHECKER_PERIOD_MILLIS = 60 * 1000;
	
	private ConcurrentLinkedDeque<T> deque;

	// Keeping size here, as ConcurrentLinkedDeque.getSize() is not a constant-time operation
	// Using AtomicInteger (to get benefit of CAS processor instructions), instead of LongAdder because no much contention is expected, and in case of high contention, then reads are expected to be much more frequent than writes
	private AtomicInteger currentSize; 
	private int softMaxCapacity;
	private volatile boolean softMaxCapacityReached;
	private ScheduledExecutorService capacityCheckerExecutor;

	public ConcurrentSoftLimitedBuffer(Collection<? extends T> copyFrom, int softMaxCapacity, int capacityCheckerPeriodMillis) {
		this.softMaxCapacity = softMaxCapacity;
		this.softMaxCapacityReached = false;
		this.deque = copyFrom != null ? new ConcurrentLinkedDeque<>(copyFrom) : new ConcurrentLinkedDeque<>();
		this.currentSize = new AtomicInteger(0);
		
		capacityCheckerExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "ConcurrentBuffer:CapacityChecker"));
		capacityCheckerExecutor.scheduleAtFixedRate(new CapacityCheckerRunnable(), capacityCheckerPeriodMillis, capacityCheckerPeriodMillis, TimeUnit.MILLISECONDS);
		
		LOGGER.trace("Constructed");
	}

	public ConcurrentSoftLimitedBuffer(int softMaxCapacity) {
		this(softMaxCapacity, DEFAULT_CAPACITY_CHECKER_PERIOD_MILLIS);
	}

	public ConcurrentSoftLimitedBuffer(int softMaxCapacity, int capacityCheckerPeriodMillis) {
		this(null, softMaxCapacity, capacityCheckerPeriodMillis);
	}
	
	private class CapacityCheckerRunnable implements Runnable {
		public void run() {
			if (currentSize.get() > softMaxCapacity) {
				softMaxCapacityReached = true;
				capacityCheckerExecutor.shutdown();
				do {
					deque.removeLast();
					currentSize.decrementAndGet();
				} while (currentSize.get() > softMaxCapacity);
				LOGGER.debug("Soft Max Capacity exceeded ({}). Not accepting more elements, extra elements have been removed", softMaxCapacity);
			}
		}
	}
	
	/**
	 * @see {@link ConcurrentLinkedDeque#addFirst(Object)}
	 * @see ConcurrentLinkedDeque#addFirst 
	 */
	public void addNew(T e) {
		deque.addFirst(e);
		LOGGER.trace("New element added: [{}]", e);

		if (softMaxCapacityReached) {
			deque.removeLast();
			LOGGER.trace("Max capacity reached, oldest removed. Size: {}", currentSize.get());
		}
		else {
			int newSize = currentSize.incrementAndGet();
			LOGGER.trace("Incremented size to: [{}]", newSize);
		}
	}

	public T peekNewest() {
		return deque.peekFirst();
	}
	
	/**
	 * As many as 'n'
	 * If there are no 'n' elements only 'n' are returned, without any error
	 * 
	 * @param n
	 * @return
	 */
	public List<T> peekNewest(int n) {
		return deque.stream().limit(n).collect(Collectors.toList());
	}
	
	public T peekOldest() {
		return deque.peekLast();
	}
	
	/**
	 * This method shouldn't be used concurrently
	 * 
	 * @param c
	 * @return
	 */
	public boolean addAllAsOldest(Collection<? extends T> c) {
		if (softMaxCapacityReached) {
			LOGGER.debug("addAllAsOldest: max capacity reached, rejecting adding more elements as oldest");
			return false;
		}
		
		deque.addAll(c);
		int newSize = currentSize.accumulateAndGet(c.size(), Integer::sum);
		LOGGER.debug("Added {} elements as oldest. New size is: {}", c.size(), newSize);

		return true;
	}
	
	/**
	 * Approximate size, if you need, you can call {@link #peekNewest(int)}, and get the size from it
	 * @return
	 */
	public int getSize() {
		return currentSize.get();
		
	}

	public int getSoftMaxCapacity() {
		return softMaxCapacity;
	}
}
