package net.rubenmartinez.stpc.exchange.bitso.trade;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubenmartinez.stpc.exchange.bitso.BitsoExchangeException;
import net.rubenmartinez.stpc.exchange.bitso.BitsoExchangeThreadInterruptedException;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient.TradesSort;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.domain.BitsoRestApiTrades;
import net.rubenmartinez.stpc.exchange.bitso.configuration.Configuration;
import net.rubenmartinez.stpc.exchange.bitso.trade.helper.ConcurrentSoftLimitedBuffer;
import net.rubenmartinez.stpc.exchange.bitso.trade.helper.NewTradesNotifier;
import net.rubenmartinez.stpc.exchange.domain.Trade;
import net.rubenmartinez.stpc.exchange.listener.TradeListener;

/**
 * getLastTrades is synchronized
 * if you need performance, just add use listener
 * 
 *
 */
public class TradesHolder implements TradeListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(TradesHolder.class);
	
	private static final int INCREASING_BUFFER_TRYLOCK_TIMEOUT_MILLIS = Configuration.getTradeHolderIterationsMillisWaitFindingLastTrade();
	
	private String bookName;
	private BitsoRestApiClient restClient;

	private NewTradesNotifier tradesNotifier;
	private volatile ConcurrentSoftLimitedBuffer<Trade> tradesBuffer;
	private ReentrantLock increasingBufferLock;
	private boolean started;

	public TradesHolder(BitsoRestApiClient client, String bookName) {
		this.bookName = bookName;
		this.restClient = client;
		this.tradesBuffer = new ConcurrentSoftLimitedBuffer<>(Configuration.getTradeHolderBufferMaxTrades());
		this.tradesNotifier = new NewTradesNotifier(client, bookName); // The exercise specifically instructs to "Use the REST API (not the websocket) to poll for recent trades at some regular interval", although normally we would be using websocket connection for that 
		this.increasingBufferLock = new ReentrantLock();
		this.started = false;
		checkConfig();
		
		LOGGER.debug("Constructed");
	}
	
	public void start() {
		synchronized (this) {
			if (!started) {
				tradesNotifier.addTradeListener(this);
				tradesNotifier.start();
				started = true;
				LOGGER.debug("Started");
			}
			else {
				LOGGER.warn("Already started");
			}
		}
	}
	
	@Override
	public void onNewTrade(Trade trade) {
		tradesBuffer.addNew(trade);
	}	

	
	public void addTradeListener(TradeListener listener) {
		tradesNotifier.addTradeListener(listener);
	}

	public void removeTradeListener(TradeListener listener) {
		tradesNotifier.removeTradeListener(listener);
	}
	
	/**
	 * Get Last <code>n</code> trades ordered from the most recent trade to oldest.
	 * That is, the element on position [0] is the most recent in time.
	 * <p>
	 * This method is thread-safe and it will return trades directly from memory most of the times
	 * so it can support multiple parallel callers without stressing Bitso.</p> 
	 * <p>
	 * The trades are stored in a memory buffer, so for example if a first caller wants to get 100 trades
	 * they are retrieved from Bitso exchange and stored in the buffer, but then all subsequent callers (even in parallel)
	 * that request 100 trades will obtain the trades directly from memory without calling Bitso.</p>
	 * <p>
	 * This is possible also because new live trades being stored also in the same buffer just as they are generated
	 * (as this class is also a {@link TradeListener})</p>
	 * 
	 * @param n number of trades to receive
	 * @return last <code>n</code> trades ordered from the most recent trade to oldest
	 */
	public List<Trade> getLastTrades(int n) {
		LOGGER.debug("getLastTrades({})", n);
		if (n > Configuration.getTradeHolderBufferMaxTrades()) {
			// If the Strategy is running for days, probably we don't want to store 1million trades in the cache buffer
			// so we just put a limit by configuration for our circular buffer
			throw new UnsupportedOperationException("Max number of trades to request from this holder are: " + Configuration.getTradeHolderBufferMaxTrades()
			                                       + ". This number can be simply increased by configuration");
		}
		
		List<Trade> lastTrades = tradesBuffer.peekNewest(n);
		LOGGER.debug("Number of trades returned from buffer: {}", lastTrades.size());
		
		if (lastTrades.size() < n) {
			lastTrades = getLastTradesIncreasingBufferSize(lastTrades.size(), n);
		}
				
		return lastTrades;
	}
	
	private List<Trade> getLastTradesIncreasingBufferSize(int currentSize, int requiredSize) {
		List<Trade> lastTrades;
		
		boolean lockAcquired = false;
		try {
			do {
				// Lock to ensure the oldestTradeId is fixed without any other thread messing with the head of the buffer
				LOGGER.debug("Trying to acquire increasingBufferLock");
				lockAcquired = increasingBufferLock.tryLock(INCREASING_BUFFER_TRYLOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
				LOGGER.debug("Trying to acquire increasingBufferLock: {}", lockAcquired);
				if (!lockAcquired) {
					
					// If lock couldn't be acquired let's see if other thread might have increased the buffer enough anyway for this thread's needs
					// Note that if some client asks for a huge number of Trades, it might take a long time to retrieve it from exchange
					// Imagine current buffer size is: 100, a Client A asks for 100000 trades, then another Client B, after, asks for 101 trades
					// Client B would be waiting a long time for client A, but this iterative tryLock client B will get 101 trades after the first 
					// iterative retrieval from Client A
					lastTrades = tradesBuffer.peekNewest(requiredSize);
					LOGGER.debug("Peeking for {} trades (just in case other thread already got them), returned {} trades", requiredSize, lastTrades.size());
				}
				else {
					String oldestTradeId = tradesBuffer.peekOldest().getTradeId();
					int extraSize = requiredSize - currentSize;
					retrieveOlderTradesFromExchangeIterativelyIntoBuffer(oldestTradeId, extraSize);
					lastTrades = tradesBuffer.peekNewest(requiredSize);
					LOGGER.debug("Buffer increased to {} trades", lastTrades.size());
				}
			} while (!lockAcquired && lastTrades.size() < requiredSize);

		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			throw new BitsoExchangeThreadInterruptedException("Interrupted while trying to get lock to increase buffer", interruptedException);
		} catch (Exception e) {
			throw new BitsoExchangeException("Unexpected exception while increasing trades buffer", e);
		} finally {
			if (lockAcquired) {
				increasingBufferLock.unlock();
			}
		}
		
		return lastTrades;
	}
	
	/**
	 * This can be done while other threads can still be calling {@link #getLastTrades(int)} 
	 * for a number of trades that are already in the buffer
	 * 
	 * @param restClient
	 * @param bookName
	 * @param currentBuffer
	 * @param extraSize
	 * @return
	 */
	private void retrieveOlderTradesFromExchangeIterativelyIntoBuffer(String oldestTradeId, int extraSize) {
		int totalTradesAdded = 0;

		LOGGER.debug("Retrieving at least {} older trades from tradeId: {}", extraSize, oldestTradeId);

		do {
			LOGGER.debug("Sleeping Configuration.getTradeHolderIterationsMillisWaitFindingLastTrade(): {}", Configuration.getTradeHolderIterationsMillisWaitFindingLastTrade());
			sleep(Configuration.getTradeHolderIterationsMillisWaitFindingLastTrade());
			List<BitsoRestApiTrades.BitsoTrade> batchFromOldestIdTrades = restClient.getTrades(bookName, oldestTradeId, TradesSort.DESC, Configuration.getTradeHolderPollTrades()).getPayload();

			tradesBuffer.addAllAsOldest(batchFromOldestIdTrades);
			totalTradesAdded += batchFromOldestIdTrades.size();
			oldestTradeId = tradesBuffer.peekOldest().getTradeId();
			
			LOGGER.debug("tradesList increased in {} trades. New oldest trade id: {}; Total trades added in this retrieval: {}", batchFromOldestIdTrades.size(), oldestTradeId, totalTradesAdded);
		} while (totalTradesAdded < extraSize);
	}
	
	private static void sleep(long millis) {
		try {
			LOGGER.trace("Sleeping");
			Thread.sleep(millis);
			LOGGER.trace("Waking up");
		} catch (InterruptedException e) {
			LOGGER.warn("Sleep interrupted");
			Thread.currentThread().interrupt();
		}
	}	
	
	private static void checkConfig() {
		if (Configuration.getTradeHolderBufferMaxTrades() <= 0) {
			throw new IllegalArgumentException("Configuration for 'trades holder buffer max trades' must be greater than zero. Current: " + Configuration.getTradeHolderBufferMaxTrades());
		}
		if (Configuration.getTradeHolderIterationsMillisWaitFindingLastTrade() <= 0) {
			throw new IllegalArgumentException("Configuration for 'trades holder iteration millis wait finding last trade' must be greater than zero. Current: " + Configuration.getTradeHolderIterationsMillisWaitFindingLastTrade());
		}
	}


}
