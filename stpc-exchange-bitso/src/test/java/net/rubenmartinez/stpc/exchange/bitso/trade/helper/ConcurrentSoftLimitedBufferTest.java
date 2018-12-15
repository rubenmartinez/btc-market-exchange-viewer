package net.rubenmartinez.stpc.exchange.bitso.trade.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jodah.concurrentunit.Waiter;
import net.rubenmartinez.stpc.test.util.TestLoggingExtension;

@ExtendWith(TestLoggingExtension.class)
public class ConcurrentSoftLimitedBufferTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentSoftLimitedBufferTest.class);
	
	@SuppressWarnings("unused") // Used inside annotations @MethodSource actually
	private static Stream<ConcurrentSoftLimitedBuffer<String>> buffersCapacity_50_Periods_100_60000() {
		return Stream.of(
				new ConcurrentSoftLimitedBuffer<>(50, 100),
				new ConcurrentSoftLimitedBuffer<>(50, 60 * 1000)
		);
	}

	@SuppressWarnings("unused") // Used inside annotations @MethodSource
	private static Stream<ConcurrentSoftLimitedBuffer<String>> buffersWithCapacity_3_Periods_100_60000() {
		return Stream.of(
				new ConcurrentSoftLimitedBuffer<>(3, 100),
				new ConcurrentSoftLimitedBuffer<>(3, 60 * 1000)
		);
	}
	
	@SuppressWarnings("unused") // Used inside annotations @MethodSource
	private static Stream<ConcurrentSoftLimitedBuffer<Integer>> buffersWithCapacity_50_3_Periods_100_60000() {
		return Stream.of(
				new ConcurrentSoftLimitedBuffer<>(50, 100),
				new ConcurrentSoftLimitedBuffer<>(50, 60 * 1000),
				new ConcurrentSoftLimitedBuffer<>(2, 100),
				new ConcurrentSoftLimitedBuffer<>(2, 60 * 1000)
		);
	}	
	
	private Waiter readerThreadWaiter = new Waiter();
	
	private class ReadFromBufferThread extends Thread {
		private ConcurrentSoftLimitedBuffer<Integer> testedBuffer;
		private int testedBufferCapacity;
		private CountDownLatch writerFinished;
		private long readSleepMills;
		private int writerIterations;
		
		public ReadFromBufferThread(ConcurrentSoftLimitedBuffer<Integer> testedBuffer, int testedBufferCapacity, CountDownLatch writerFinished, long readSleepMillis, int writerIterations) {
			this.testedBuffer = testedBuffer;
			this.testedBufferCapacity = testedBufferCapacity;
			this.writerFinished = writerFinished;
			this.readSleepMills = readSleepMillis;
			this.writerIterations = writerIterations;
		}
		
		@Override
		public void run() {
			try {
				while (writerFinished.getCount() > 0) {
					List<Integer> elements = testedBuffer.peekNewest(testedBufferCapacity);
					assertSequentialElements(elements);
					LOGGER.debug("Buffer read: {} (size: {})", elements, elements.size());
					sleepMillis(readSleepMills);
				}
				
				// If writer finished, check final state is correct
				List<Integer> elements = testedBuffer.peekNewest(testedBufferCapacity);
				LOGGER.debug("Finished - Buffer read: {} (size: {})", elements, elements.size());

				assertEquals(testedBufferCapacity, elements.size());
				assertEquals(writerIterations-1, (int) testedBuffer.peekNewest());
				assertEquals(writerIterations-testedBufferCapacity, (int) elements.get(testedBufferCapacity-1));
				
				assertThat(testedBuffer.peekOldest(), anyOf(is(writerIterations-testedBufferCapacity), is(0)));
				assertThat(testedBuffer.peekNewest(2), contains(99, 98));
				if (testedBufferCapacity > 10) {
					assertThat(testedBuffer.peekNewest(10), contains(99, 98, 97, 96, 95, 94, 93, 92, 91, 90));
				}
			} finally {
				readerThreadWaiter.resume();
			}
		}
		
		private void assertSequentialElements(List<Integer> elements) {
			Integer previousElement = null;
			for (Integer element: elements) {
				if (previousElement != null) {
					assertEquals((int) (previousElement-1), (int) element);
				}
			}
			
		}
	}	
	

	@ParameterizedTest
	@MethodSource("buffersCapacity_50_Periods_100_60000")
	void testBasicMonoThread(ConcurrentSoftLimitedBuffer<String> testedBuffer) {
		testedBuffer.addNew("1");
		assertEquals(1, testedBuffer.getSize());
		assertEquals("1", testedBuffer.peekNewest());
		assertEquals("1", testedBuffer.peekOldest());
		
		testedBuffer.addNew("2");
		assertEquals(2, testedBuffer.getSize());
		assertEquals("2", testedBuffer.peekNewest());
		assertEquals("1", testedBuffer.peekOldest());
		assertEquals(2, testedBuffer.peekNewest(2).size());
		assertThat(testedBuffer.peekNewest(2), contains("2", "1"));
		assertEquals(2, testedBuffer.peekNewest(10).size());
		assertThat(testedBuffer.peekNewest(10), contains("2", "1"));

		testedBuffer.addAllAsOldest(Arrays.asList("0", "-1"));
		assertEquals(4, testedBuffer.getSize());
		assertEquals("2", testedBuffer.peekNewest());
		assertEquals("-1", testedBuffer.peekOldest());
		assertThat(testedBuffer.peekNewest(10), contains("2", "1", "0", "-1"));
		
		testedBuffer.addNew("3");
		sleepMillis(500);
		testedBuffer.addNew("4");
		sleepMillis(500);
		testedBuffer.addAllAsOldest(Arrays.asList("-2", "-3"));
		sleepMillis(500);
		assertEquals(8, testedBuffer.getSize());
		assertEquals("4", testedBuffer.peekNewest());
		assertEquals("-3", testedBuffer.peekOldest());
		assertThat(testedBuffer.peekNewest(10), contains("4", "3", "2", "1", "0", "-1", "-2", "-3"));
	}

	@ParameterizedTest
	@MethodSource("buffersWithCapacity_3_Periods_100_60000")
	void testBasicMonoThreadCapacityLimited(ConcurrentSoftLimitedBuffer<String> testedBuffer) {
		testedBuffer.addNew("1");
		assertEquals(1, testedBuffer.getSize());
		assertEquals("1", testedBuffer.peekNewest());
		assertEquals("1", testedBuffer.peekOldest());
		
		testedBuffer.addNew("2");
		assertEquals(2, testedBuffer.getSize());
		assertEquals("2", testedBuffer.peekNewest());
		assertEquals("1", testedBuffer.peekOldest());
		assertEquals(2, testedBuffer.peekNewest(2).size());
		assertThat(testedBuffer.peekNewest(2), contains("2", "1"));
		assertEquals(2, testedBuffer.peekNewest(10).size());
		assertThat(testedBuffer.peekNewest(10), contains("2", "1"));

		testedBuffer.addAllAsOldest(Arrays.asList("0", "-1"));
		assertThat(testedBuffer.getSize(), greaterThanOrEqualTo(3));
		assertThat(testedBuffer.getSize(), lessThanOrEqualTo(4));
		assertThat(testedBuffer.peekNewest(10), anyOf(contains("2", "1", "0"), contains("2", "1", "0", "-1")));
		
		LOGGER.debug("testedBuffer (1): {}", testedBuffer.peekNewest(10));
		
		testedBuffer.addNew("3");
		sleepMillis(500);
		testedBuffer.addNew("4");
		sleepMillis(500);
		testedBuffer.addAllAsOldest(Arrays.asList("-2", "-3"));
		sleepMillis(500);
		LOGGER.debug("testedBuffer (2): {}", testedBuffer.peekNewest(10));

		
		assertThat(testedBuffer.peekNewest(10), anyOf(contains("4", "3", "2"), contains("4", "3", "2", "1", "0", "-1", "-2", "-3")));
		
	}
	
	@Test
	void testAddOldIgnoredWhenCapacityReached() {
		ConcurrentSoftLimitedBuffer<String> testedBuffer = new ConcurrentSoftLimitedBuffer<>(3, 1);
		testedBuffer.addNew("1");
		testedBuffer.addNew("2");
		testedBuffer.addNew("3");
		testedBuffer.addNew("4");
		testedBuffer.addNew("5");
		sleepMillis(200);
		assertEquals("5", testedBuffer.peekNewest());
		assertEquals("3", testedBuffer.peekOldest());
		assertEquals(3, testedBuffer.peekNewest(10).size());
		boolean result = testedBuffer.addAllAsOldest(Arrays.asList("ignored", "ignored"));
		assertFalse(result);
		assertThat(testedBuffer.peekNewest(10), not(contains("ignored")));

		testedBuffer = new ConcurrentSoftLimitedBuffer<>(3, 1);
		testedBuffer.addAllAsOldest(Arrays.asList("1", "2", "3", "4", "5", "6"));
		sleepMillis(200);
		assertEquals("1", testedBuffer.peekNewest());
		assertEquals("3", testedBuffer.peekOldest());
		assertEquals(3, testedBuffer.peekNewest(10).size());
		result = testedBuffer.addAllAsOldest(Arrays.asList("ignored", "ignored"));
		assertFalse(result);
		assertThat(testedBuffer.peekNewest(10), not(contains("ignored")));
		
		testedBuffer = new ConcurrentSoftLimitedBuffer<>(3, 1);
		testedBuffer.addNew("1");
		testedBuffer.addNew("2");
		testedBuffer.addAllAsOldest(Arrays.asList("0", "-1", "-2", "-3"));
		sleepMillis(200);
		assertEquals("2", testedBuffer.peekNewest());
		assertEquals("0", testedBuffer.peekOldest());
		assertEquals(3, testedBuffer.peekNewest(10).size());
		result = testedBuffer.addAllAsOldest(Arrays.asList("ignored", "ignored"));
		assertFalse(result);
		assertThat(testedBuffer.peekNewest(10), not(contains("ignored")));
	}
	
	@ParameterizedTest
	@MethodSource("buffersWithCapacity_50_3_Periods_100_60000")
	void getBufferConcurrentAccess(ConcurrentSoftLimitedBuffer<Integer> testedBuffer) throws InterruptedException, TimeoutException {
		int NUM_READER_THREADS = 20;
		int WRITER_ITERATIONS = 100;
		long WRITER_SLEEP_MILLIS = 20;
		long READER_SLEEP_MILLIS = 5;
		long READERS_WAIT_TIMEOUT_SECONDS = 120;
		
		CountDownLatch writerFinishedLath = new CountDownLatch(1);
		
		Stream.generate(() -> new ReadFromBufferThread(testedBuffer, testedBuffer.getSoftMaxCapacity(), writerFinishedLath, READER_SLEEP_MILLIS, WRITER_ITERATIONS))
		      .limit(NUM_READER_THREADS)
		      .forEach(thread -> thread.start());
		
		for (int i = 0; i < WRITER_ITERATIONS; i++) {
			LOGGER.debug("About to add element: {}", i);
			testedBuffer.addNew(i);
			List<Integer> elements = testedBuffer.peekNewest(testedBuffer.getSoftMaxCapacity());
			LOGGER.debug("element added: {}, buffer is: {} (size: {})", i, elements, elements.size());
			
			TimeUnit.MILLISECONDS.sleep(WRITER_SLEEP_MILLIS);
		}
		writerFinishedLath.countDown();
		
		readerThreadWaiter.await(READERS_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS, NUM_READER_THREADS);
	}	
	
	private static void sleepMillis(long millis) {
		// This should be substituted: Tests could fail under some circumstances as they have some assumptions on async code durations, even if the "timeouts" should be high enough, on slow machines a timeout for an async code could trigger so the test is assumed to fail, even if the code is working correctly.
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			LOGGER.warn("Sleep interrupted");
			Thread.currentThread().interrupt();
		}
	}		
}
