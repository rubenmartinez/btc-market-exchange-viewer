package net.rubenmartinez.stpc.app.exchange.decorator;

import java.math.BigDecimal;
import java.util.List;

import net.rubenmartinez.stpc.exchange.ExchangeClient;
import net.rubenmartinez.stpc.exchange.domain.Order;
import net.rubenmartinez.stpc.exchange.domain.OrderBook;
import net.rubenmartinez.stpc.exchange.domain.OrderSide;
import net.rubenmartinez.stpc.exchange.domain.Trade;
import net.rubenmartinez.stpc.exchange.listener.TradeListener;

public class ExchangeClientDecorator implements ExchangeClient {
	
	protected ExchangeClient exchangeClient;
	
	public ExchangeClientDecorator(ExchangeClient exchangeClient) {
		this.exchangeClient = exchangeClient;
	}

	@Override
	public String placeLimitOrder(OrderSide side, BigDecimal price, BigDecimal amount) {
		return exchangeClient.placeLimitOrder(side, price, amount);
	}

	@Override
	public OrderBook getOrderBook() {
		return exchangeClient.getOrderBook();
	}

	@Override
	public List<Order> getBids(int n) {
		return exchangeClient.getBids(n);
	}

	@Override
	public List<Order> getAsks(int n) {
		return exchangeClient.getAsks(n);
	}

	/**
	 * @see ExchangeClient#getLastTrades(int)
	 */
	@Override
	public List<Trade> getLastTrades(int n) {
		return exchangeClient.getLastTrades(n);
	}

	@Override
	public void addTradeListener(TradeListener listener) {
		exchangeClient.addTradeListener(listener);
	}

	@Override
	public void removeTradeListener(TradeListener listener) {
		exchangeClient.removeTradeListener(listener);
	}
}