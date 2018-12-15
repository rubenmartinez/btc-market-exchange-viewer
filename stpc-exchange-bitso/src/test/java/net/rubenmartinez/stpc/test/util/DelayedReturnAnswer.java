package net.rubenmartinez.stpc.test.util;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mockito {@link Answer} that sleeps for the specified amount of time before returning the mocked value
 * Just utility class for better code readability
 *
 * @param <T> the type to return.
 */
public class DelayedReturnAnswer<T> implements Answer<T> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DelayedReturnAnswer.class);
	
	private Duration delay;
	private T value;
	
	public DelayedReturnAnswer(Duration delay, T value) {
		this.delay = delay;
		this.value = value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T answer(InvocationOnMock invocation) throws Throwable {
		if (!delay.isZero()) {
			TimeUnit.MILLISECONDS.sleep(delay.toMillis());
		}
		
		LOGGER.info("Returning [{}] after a delay of ", value, delay);
		return value;
	}

}
