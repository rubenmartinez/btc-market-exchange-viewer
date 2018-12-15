package net.rubenmartinez.stpc.exchange.bitso.trade.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient.TradesSort;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.domain.BitsoRestApiTrades;
import net.rubenmartinez.stpc.exchange.bitso.configuration.Configuration;
import net.rubenmartinez.stpc.exchange.domain.Trade;
import net.rubenmartinez.stpc.exchange.listener.TradeListener;

/**
 * Normally, we would be using the websocket connection to receive the last trades from Bitso
 * But the exercise specifically instructs to Use the REST API (not the websocket) to poll for recent trades at some regular interval,
 * so this is an internal helper class to comply with this, and it will be also "converting" 
 * the polling into Trades notifications for listeners so don't have to worry about the polling for new trades.
 */
public class NewTradesNotifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(NewTradesNotifier.class);

	private ScheduledExecutorService executor;
	
	private String bookName;
	private BitsoRestApiClient restClient;

	private String lastTradeId;
	private List<TradeListener> tradeListeners;
	
	private boolean started;

	public NewTradesNotifier(BitsoRestApiClient client, String bookName) {
		this.bookName = bookName;
		this.restClient = client;
		this.tradeListeners = new CopyOnWriteArrayList<>();
		this.started = false;
		checkConfig();

		LOGGER.debug("Constructed");
	}
	
	/**
	 * Starts the polling from Bitso and notifying clients of new trades. 
	 * First time start is called, all listeners already subscribed are notified of one trade (the most recent one).
	 */
	public void start() {
		synchronized (this) {
			if (!started) {
				this.lastTradeId = initializeLastTradeId();
				
				int pollSeconds = Configuration.getTradeHolderPollSeconds();

				executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "NewTrades:scheduler"));
				Runnable retrieveRecentTradesAndNotify = () -> {
					List<Trade> tradesToNotify = findNewTradesInAscendingOrder();
					if (!tradesToNotify.isEmpty()) {
						notifyListeners(tradesToNotify);
					}
				};
				executor.scheduleAtFixedRate(retrieveRecentTradesAndNotify, pollSeconds, pollSeconds, TimeUnit.SECONDS);
				started = true;
				LOGGER.debug("Started");
			}
			else {
				LOGGER.warn("Already started");
			}
		}
	}
	
	/**
	 * Stops the polling and notifying clients of new trades
	 */
	public void stop() {
		synchronized (this) {
			if (started) {
				executor.shutdown();
				started = false;
			}
			else {
				LOGGER.warn("Already stopped");
			}
		}
	}

	/**
	 * Adds a {@link TradeListener}, which will be notified whenever a new Trade is found in Bitso.
	 * If the same listener is added more than once, then it will be notified more than once. That is, no check is made to ensure uniqueness.
	 * 
	 * @param listener The listener to register
	 */
	public void addTradeListener(TradeListener listener) {
		tradeListeners.add(listener);
		LOGGER.debug("Added listener: {}", listener);
	}

	/**
	 * Removes the given listener from the list of listeners
	 * 
	 * @param listener The listener to remove
	 */
	public void removeTradeListener(TradeListener listener) {
		tradeListeners.remove(listener);
		LOGGER.debug("Removed listener: {}", listener);
	}
	
	
	
	private String initializeLastTradeId() {
		List<BitsoRestApiTrades.BitsoTrade> trades = new Retry()
				.withDelay(Configuration.getTradeHolderPollSeconds(), TimeUnit.SECONDS)
				.withExceptionMessage("Error while getting last trade id. That is required for initialization so waiting for " + Configuration.getTradeHolderPollSeconds() + " seconds (same as poll interval) and retrying")
				.indefinitely(() -> restClient.getNewestTrades(bookName, 1).getPayload());
		
		Trade lastTrade = trades.get(0);
		notifyListeners(lastTrade);
		return lastTrade.getTradeId();
	}
	
	private void notifyListeners(List<Trade> trades) {
		LOGGER.debug("Notifying listeners of <{}> new trades starting with id: {}", trades.size(), trades.get(0).getTradeId());
		tradeListeners.forEach(listener -> trades.forEach(trade -> safeNotification(listener, trade)));
		LOGGER.info("Listeners notified of <{}> new trades", trades.size());
	}


	private void notifyListeners(Trade trade) {
		LOGGER.debug("Notifying listeners of Trade: {}", trade);
		tradeListeners.forEach(listener -> safeNotification(listener, trade));
		LOGGER.info("Listeners notified of trade: {}", trade);
	}

	private static void safeNotification(TradeListener listener, Trade trade) {
		try {
			LOGGER.debug("Notifying listener [{}]. Trade: {}", listener, trade);
			listener.onNewTrade(trade);
		} catch (Exception e) {
			LOGGER.warn("(Swallowing) Exception on listener ["+listener+"] while notifying of trade: " + trade, e);
		}
	}

	private List<BitsoRestApiTrades.BitsoTrade> tryToFindNewTradesFromLastTradeIdAscending() {
		return restClient.getTrades(bookName, lastTradeId, TradesSort.ASC, Configuration.getTradeHolderPollTrades()).getPayload();
	}

	/**
	 * Uses Bitso REST Client to get the last trades from the last one notified.
	 * As Bitso has a maximum number of trades to provide in each REST Call (currently 100), but in some circumstances it could happen 
	 * that from the last poll, more than 100 trades are done 
	 * To keep up with his promise, this method will iterate (with a delay) till it makes sure that all the last trades from last one are retrieved and notified.
	 * 
	 * @return the last trades from the last one notified 
	 */
	private List<Trade> findNewTradesInAscendingOrder() {
		LOGGER.debug("Looking for new trades, last trade id is: {}", lastTradeId);

		List<Trade> newTrades = new ArrayList<>();

		List<BitsoRestApiTrades.BitsoTrade> batchFromLastIdTrades;
		boolean scanCompleted = false;
		do {
			batchFromLastIdTrades = tryToFindNewTradesFromLastTradeIdAscending();
			LOGGER.debug("Iteration returned {} new trades", batchFromLastIdTrades.size());
			if (!batchFromLastIdTrades.isEmpty()) {
				newTrades.addAll(batchFromLastIdTrades);
				lastTradeId = getLast(newTrades).getTradeId();
			}
			
			if (batchFromLastIdTrades.size() < Configuration.getTradeHolderPollTrades()) {
				scanCompleted = true;
			}
			else {
				LOGGER.debug("Number of new trades returned are the same as requested ({}). Performing another iteration to check if there are newer trades", Configuration.getTradeHolderPollTrades());
				sleep(Configuration.getTradeHolderIterationsMillisWaitFindingLastTrade());
			}
		} while (!scanCompleted);

		LOGGER.debug("Found {} new trades, lastTradeId is now: {}", newTrades.size(), lastTradeId);
		return newTrades;
	}

	private static final <T> T getLast(List<T> list) {
		return list.get(list.size() - 1);
	}

	private static void sleep(long millis) {
		try {
			TimeUnit.MILLISECONDS.sleep(millis);
		} catch (InterruptedException e) {
			LOGGER.warn("Sleep interrupted");
			Thread.currentThread().interrupt();
		}
	}

	private static void checkConfig() {
		if (Configuration.getTradeHolderPollSeconds() <= 0) {
			throw new IllegalArgumentException("Configuration for 'trades holder poll seconds' must be greater than zero. Current: " + Configuration.getTradeHolderPollSeconds());
		}
		if (Configuration.getTradeHolderPollTrades() <= 0) {
			throw new IllegalArgumentException("Configuration for 'trades holder poll trades' must be greater than zero. Current: " + Configuration.getTradeHolderPollTrades());
		}
	}

}
