package net.rubenmartinez.stpc.exchange.bitso.trade.helper;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Just a very simple Retry helper, to do retries with a more functional style and to avoid a dependency on a complex retry library.
 * 
 * There are some operations that had to be retried indefinitely, for example if the Trades Notifier cannot retrieve the last tradeId
 * at initialization phase (because no internet connection for example), then the periodic scheduler won't be able to get newer trades based on that last trade.
 *
 */
public class Retry {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Retry.class);
	
	private String exceptionMessage;
	private long delayMillis;
	
	public Retry withDelay(long delay, TimeUnit timeUnit) {
		this.delayMillis = timeUnit.toMillis(delay);
		return this;
	}
	
	public Retry withExceptionMessage(String message) {
		this.exceptionMessage = message;
		return this;
	}
	
	public <T> T indefinitely(Supplier<T> supplier) {
		do {
			try {
				return supplier.get();
			} catch (Exception e) {
				if (exceptionMessage != null) {
					LOGGER.warn("exceptionMessage", e);
				}
				if (delayMillis > 0) {
					sleep(delayMillis);
				}
			}
		} while (true);
	}
	
	private static void sleep(long millis) {
		try {
			TimeUnit.MILLISECONDS.sleep(millis);
		} catch (InterruptedException e) {
			LOGGER.warn("Sleep interrupted");
			Thread.currentThread().interrupt();
		}
	}	
}
