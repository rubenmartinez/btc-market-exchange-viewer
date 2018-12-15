package net.rubenmartinez.stpc.exchange.bitso.orderbook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jodah.concurrentunit.Waiter;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.domain.BitsoOrder;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.helper.SortedBookOrdersMap;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.helper.SortedBookOrdersMap.SortOrdering;
import net.rubenmartinez.stpc.exchange.domain.Order;
import net.rubenmartinez.stpc.test.util.TestLoggingExtension;

@ExtendWith(TestLoggingExtension.class)
public class SortedBookOrdersMapTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(SortedBookOrdersMapTest.class);
	
	private SortedBookOrdersMap sortedBookOrdersMap;
	private Waiter readerThreadWaiter = new Waiter();
	
	@BeforeEach
	void beforeEach() {
		sortedBookOrdersMap = new SortedBookOrdersMap(SortOrdering.ASCENDING);
	}

	private BitsoOrder getOrderFromMap(String orderId) {
		return sortedBookOrdersMap.get(orderId).orElseThrow(() -> new IllegalArgumentException("Order with id [" + orderId + "] not present in map"));
	}
	
	private class ReadOrdersThread extends Thread {
		private CountDownLatch writerFinished;
		private long readSleepMills;
		private int writerIterations;
		
		public ReadOrdersThread(CountDownLatch writerFinished, long readSleepMillis, int writerIterations) {
			this.writerFinished = writerFinished;
			this.readSleepMills = readSleepMillis;
			this.writerIterations = writerIterations;
		}
		
		@Override
		public void run() {
			try {
				while (writerFinished.getCount() > 0) {
					List<Order> orders = sortedBookOrdersMap.getSortedOrdersThreadSafe();
					int size = orders.size();
					
					LOGGER.debug("orders size read: {}", size);
					
					checkSequentialNoMissingOrderTillIndex(orders, size);
					orders = sortedBookOrdersMap.getBestNSortedOrdersThreadSafe(size);
					checkSequentialNoMissingOrderTillIndex(orders, size);
					
					sleepMillis(readSleepMills);
				}
				
				// If writer finished, check all threads will read all orders
				List<Order> orders = sortedBookOrdersMap.getSortedOrdersThreadSafe();
				checkSequentialNoMissingOrderTillIndex(orders, writerIterations);
				orders = sortedBookOrdersMap.getBestNSortedOrdersThreadSafe(writerIterations);
				checkSequentialNoMissingOrderTillIndex(orders, writerIterations);
				
			} finally {
				readerThreadWaiter.resume();
			}
		}
		
		private final void sleepMillis(long millis) {
			try {
				TimeUnit.MILLISECONDS.sleep(millis);
			} catch (InterruptedException e) {
				throw new IllegalStateException("Unexpected InterruptedException", e);
			}
		}
	}	

	@Test
	void duplicateId_SubstituteExistingOrder() {
		sortedBookOrdersMap.put(new BitsoOrder("id", "100", "1"));
		assertEquals(1, sortedBookOrdersMap.size());
		assertEquals("100", getOrderFromMap("id").getPrice());
		assertEquals("1", getOrderFromMap("id").getAmount());

		sortedBookOrdersMap.put(new BitsoOrder("id", "100", "1"));
		assertEquals(1, sortedBookOrdersMap.size());
		assertEquals("100", getOrderFromMap("id").getPrice());
		assertEquals("1", getOrderFromMap("id").getAmount());

		sortedBookOrdersMap.put(new BitsoOrder("id", "120", "1"));
		assertEquals(1, sortedBookOrdersMap.size());
		assertEquals("120", getOrderFromMap("id").getPrice());
		assertEquals("1", getOrderFromMap("id").getAmount());

		sortedBookOrdersMap.put(new BitsoOrder("id", "100", "10"));
		assertEquals(1, sortedBookOrdersMap.size());
		assertEquals("100", getOrderFromMap("id").getPrice());
		assertEquals("10", getOrderFromMap("id").getAmount());
	}

	@Test
	void differentIdSamePrice_DifferentOrders() {
		sortedBookOrdersMap.put(new BitsoOrder("id", "100", "1"));
		assertEquals(1, sortedBookOrdersMap.size());
		
		assertEquals("100", getOrderFromMap("id").getPrice());
		assertEquals("1", getOrderFromMap("id").getAmount());

		sortedBookOrdersMap.put(new BitsoOrder("newid", "100", "1"));
		assertEquals(2, sortedBookOrdersMap.size());

		assertEquals("100", getOrderFromMap("id").getPrice());
		assertEquals("1", getOrderFromMap("id").getAmount());
		
		assertEquals("100", getOrderFromMap("newid").getPrice());
		assertEquals("1", getOrderFromMap("id").getAmount());
	}
	
	@Test
	void getOrdersSequentialAccess() {
		int ITERATIONS = 100;
		
		for (int i = 0; i < ITERATIONS; i++) {
			sortedBookOrdersMap.put(new BitsoOrder("id-" + i, "" + i, "1"));
			List<Order> orders = sortedBookOrdersMap.getSortedOrdersThreadSafe();
			checkSequentialNoMissingOrderTillIndex(orders, i);
			orders = sortedBookOrdersMap.getBestNSortedOrdersThreadSafe(i+1);
			checkSequentialNoMissingOrderTillIndex(orders, i);
		}
	}

	private static void checkSequentialNoMissingOrderTillIndex(List<Order> orders, int maxIndex) {
		for (int i = 0; i < maxIndex; i++) {
			String checkId = "id-" + i;
			String checkPrice = "" + i;
			assertEquals(checkId, orders.get(i).getId());
			assertEquals(checkPrice, orders.get(i).getPrice());
		}
	}

	@Test
	void getOrdersConcurrentAccess() throws InterruptedException, TimeoutException {
		int NUM_READER_THREADS = 200;
		int WRITER_ITERATIONS = 100;
		long WRITER_SLEEP_MILLIS = 20;
		long READER_SLEEP_MILLIS = 5;
		long READERS_WAIT_TIMEOUT_SECONDS = 120;
		
		CountDownLatch writerFinishedLath = new CountDownLatch(1);
		
		Stream.generate(() -> new ReadOrdersThread(writerFinishedLath, READER_SLEEP_MILLIS, WRITER_ITERATIONS))
		      .limit(NUM_READER_THREADS)
		      .forEach(thread -> thread.start());
		
		for (int i = 0; i < WRITER_ITERATIONS; i++) {
			BitsoOrder newOrder = new BitsoOrder("id-" + i, "" + i, "1");
			LOGGER.debug("about to put order: {}", newOrder);
			sortedBookOrdersMap.put(new BitsoOrder("id-" + i, "" + i, "1"));
			LOGGER.debug("order put: {}", newOrder);
			TimeUnit.MILLISECONDS.sleep(WRITER_SLEEP_MILLIS);
		}
		writerFinishedLath.countDown();
		
		readerThreadWaiter.await(READERS_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS, NUM_READER_THREADS);
		
	}
}
