package net.rubenmartinez.stpc.app.exchange.decorator;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubenmartinez.stpc.app.exchange.domain.TaggedTrade;
import net.rubenmartinez.stpc.app.exchange.service.listener.TaggedTradeListener;
import net.rubenmartinez.stpc.exchange.ExchangeClient;
import net.rubenmartinez.stpc.exchange.domain.OrderSide;
import net.rubenmartinez.stpc.exchange.domain.Trade;
import net.rubenmartinez.stpc.exchange.listener.TradeListener;

/**
 * 
 * FlaggedTrade assumed to be filled immediately
 *
 */
public class SimulatedTradesExchangeClient extends ExchangeClientDecorator implements TradeListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedTradesExchangeClient.class);

	private List<TaggedTradeListener> copyOnWriteTradeListeners;
	private List<SimulatedTrade> simulatedTrades;

	private volatile Boolean thisAddedAsListener;

	public SimulatedTradesExchangeClient(ExchangeClient exchangeClient) {
		super(exchangeClient);
		this.thisAddedAsListener = false; // Will add as a listener of the exchangeClient later as it is not safe to call super.addTradeListener(this) inside constructor as object it not ensured to be created till constructor has finished
		this.copyOnWriteTradeListeners = new CopyOnWriteArrayList<>();
		this.simulatedTrades = Collections.synchronizedList(new ArrayList<>()); // We should use a circular buffer cause the memory will if the app is running enough time, but it should be ok for the exercise  
	}

	private void placeSimulatedOrder(SimulatedTrade simulatedTrade) {
		LOGGER.debug("Simulating placeLimitOrder({})", simulatedTrade);
		simulatedTrades.add(simulatedTrade);
		notifyNewTrade(simulatedTrade);
	}
	
	public void clearSimulatedTrades(String strategyId) {
		LOGGER.debug("clearSimulatedTrades(strategyId={})", strategyId);
		simulatedTrades.removeIf(trade -> strategyId.equals(trade.getStrategyId()));
	}
	
	public void simulateBuy(String strategyId, String orderId, BigDecimal price, BigDecimal amount, ZonedDateTime creationDate) {
		placeSimulatedOrder(new SimulatedTrade(strategyId, orderId, OrderSide.SELL, price, amount, creationDate)); // Note OrderSide is the maker Side, so we have to simulate a SELL
	}

	public void simulateSell(String strategyId, String orderId, BigDecimal price, BigDecimal amount, ZonedDateTime creationDate) {
		placeSimulatedOrder(new SimulatedTrade(strategyId, orderId, OrderSide.BUY, price, amount, creationDate)); // Note OrderSide is the maker Side, so we have to simulate a SELL
	}

	public void addFlaggedTradeListener(TaggedTradeListener listener) {
		copyOnWriteTradeListeners.add(listener);
		addThisAsListenerInBaseExchangeIfNotAlreadyAdded();
	}

	public void removeFlaggedTradeListener(TaggedTradeListener listener) {
		copyOnWriteTradeListeners.remove(listener);
	}

	private void addThisAsListenerInBaseExchangeIfNotAlreadyAdded() {
		if (!thisAddedAsListener) { // double check possible as 'thisAddedAsListener' is volatile
			synchronized (this) {
				if (!thisAddedAsListener) {
					super.addTradeListener(this);
					thisAddedAsListener = true;
				}
			}
		}
	}

	@Override
	public void onNewTrade(Trade trade) {
		notifyNewTrade(new TaggedTrade(trade));
	}
	
	private void notifyNewTrade(TaggedTrade trade) {
		LOGGER.debug("Notifying listeners of new trade: {}", trade);
		copyOnWriteTradeListeners.forEach(listener -> safeNotify(listener, trade));
	}

	private static void safeNotify(TaggedTradeListener listener, TaggedTrade trade) {
		try {
			listener.onNewTrade(trade);
		} catch (Exception e) {
			LOGGER.warn("(Swallowing) Error while notifying sending TaggedTradeListener [{}] of trade: {}", listener, trade);
		}
	}
	
	/**
	 * New method in this decorator to return the last trades including the simulated trades from a particular strategy
	 * sorted in ascending order 
	 * 
	 * As the resolution of creation date is up to a second. The simulated trades are not easily shown in correct order so
	 * this decorator
	 * 
	 * It uses a "trick" that wouldn't actually work with Exchanges which don't have tradeIds that can be ordered.
	 * Avoiding this trick would be quite simple but laborious as would imply to modify a lot of code from <code>stpc-exchange-api</code>
	 * to keep track of an <em>sortId</em>, so for the moment I would keep it like this being the last day of the challenge.
	 * 
	 * As this is a simulation, this method was <em>not</em> designed for performance  
	 * 
	 * @param n The number of trades to return
	 * @return the last trades including the simulated trades from a particular strategy sorted in ascending order
	 */
	public List<TaggedTrade> getLastTradesOrderedIncludingSimulated(int n, String strategyId) {
		Stream<SimulatedTrade> simulatedTradesStream = simulatedTrades.stream().filter(trade -> strategyId.equals(trade.getStrategyId()));
		Stream<TaggedTrade> realTradesStream = super.getLastTrades(n).stream().map(TaggedTrade::new);
		
		Stream<TaggedTrade> allTradesStream = Stream.concat(simulatedTradesStream, realTradesStream).distinct();
		return allTradesStream.sorted(Comparator.comparing(TaggedTrade::getTradeId).reversed()).limit(n).collect(Collectors.toList());
	}
}
