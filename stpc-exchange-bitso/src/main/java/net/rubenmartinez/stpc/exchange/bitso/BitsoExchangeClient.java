package net.rubenmartinez.stpc.exchange.bitso;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import net.rubenmartinez.stpc.exchange.ExchangeClient;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient;
import net.rubenmartinez.stpc.exchange.bitso.api.websocket.BitsoWebsocketClient;
import net.rubenmartinez.stpc.exchange.bitso.api.websocket.DiffOrdersListener;
import net.rubenmartinez.stpc.exchange.bitso.configuration.Configuration;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.OrderBookKeeper;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.ReplayQueueOrderBookKeeper;
import net.rubenmartinez.stpc.exchange.bitso.trade.TradesHolder;
import net.rubenmartinez.stpc.exchange.domain.Order;
import net.rubenmartinez.stpc.exchange.domain.OrderBook;
import net.rubenmartinez.stpc.exchange.domain.OrderSide;
import net.rubenmartinez.stpc.exchange.domain.Trade;
import net.rubenmartinez.stpc.exchange.listener.TradeListener;

/**
 *
 */
public class BitsoExchangeClient implements ExchangeClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(BitsoExchangeClient.class);

	private OrderBookKeeper orderBookKeeper;
	private TradesHolder tradesHolder;
	private BitsoRestApiClient restApiClient;
	private BitsoWebsocketClient webSocketClient;

	/**
	 * Immediately starts a websocket client to keep an orderbook synchronized
	 * 
	 * @param bookName
	 */
	public BitsoExchangeClient(String bookName) {
		this.restApiClient = constructRestApiClient();
		
		this.orderBookKeeper = new ReplayQueueOrderBookKeeper(restApiClient, bookName);
		
		this.webSocketClient = new BitsoWebsocketClient((DiffOrdersListener) this.orderBookKeeper);
		this.webSocketClient.start();
		
		this.tradesHolder = new TradesHolder(restApiClient, bookName);
		this.tradesHolder.start();

		LOGGER.debug("Constructed");
	}
	
	private static BitsoRestApiClient constructRestApiClient() {
		String restEndpoint = Configuration.getRestEndpointUri();
		
		return Feign.builder()
				.client(new OkHttpClient())
				.logger(new Slf4jLogger()).logLevel(feign.Logger.Level.FULL)
				.decoder(new JacksonDecoder())
				.target(BitsoRestApiClient.class, restEndpoint);
	}

	@Override
	public OrderBook getOrderBook() {
		return orderBookKeeper.getOrderBook();
	}

	/**
	 * Get the current best bids in Bitso Orderbook
	 */
	@Override
	public List<Order> getBids(int n) {
		return orderBookKeeper.getBids(n);
	}

	/**
	 * Get the current best asks in Bitso Orderbook
	 */
	@Override
	public List<Order> getAsks(int n) {
		return orderBookKeeper.getAsks(n);
	}

	/**
	 * Get Last <code>n</code> trades ordered from the most recent trade to oldest.
	 * That is, the element on position [0] is the most recent in time.
	 */
	@Override
	public List<Trade> getLastTrades(int n) {
		return tradesHolder.getLastTrades(n);
	}

	@Override
	public void addTradeListener(TradeListener listener) {
		tradesHolder.addTradeListener(listener);
	}

	@Override
	public void removeTradeListener(TradeListener listener) {
		tradesHolder.removeTradeListener(listener);
	}

	@Override
	public String placeLimitOrder(OrderSide side, BigDecimal price, BigDecimal amount) {
		throw new UnsupportedOperationException("placing orders is not supported in this version");
	}
}
