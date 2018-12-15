package net.rubenmartinez.stpc.app.exchange.service;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import net.rubenmartinez.stpc.app.exchange.decorator.SimulatedTradesExchangeClient;
import net.rubenmartinez.stpc.app.exchange.domain.TaggedTrade;
import net.rubenmartinez.stpc.app.exchange.service.listener.TaggedTradeListener;
import net.rubenmartinez.stpc.exchange.ExchangeClient;
import net.rubenmartinez.stpc.exchange.domain.Order;
import net.rubenmartinez.stpc.exchange.domain.Trade;
import net.rubenmartinez.stpc.exchange.listener.TradeListener;


/**
 * 
 * This additional layer could be used to control number of requests, throttle, etc..
 * 
 * For the moment it is just a dummy layer
 * 
 *
 */
@Service
public class ExchangeService {
	
	@Resource
	private SimulatedTradesExchangeClient exchangeClient;


	/**
	 * @see ExchangeClient#getBids(int)
	 */
	public List<Order> getBids(int n) {
		return exchangeClient.getBids(n);
	}

	/**
	 * @see ExchangeClient#getAsks(int)
	 */
	public List<Order> getAsks(int n) {
		return exchangeClient.getAsks(n);
	}

	/**
	 * @see ExchangeClient#getLastTrades(int)
	 */
	public List<Trade> getLastTrades(int n) {
		return exchangeClient.getLastTrades(n);
	}

	/**
	 * @see ExchangeClient#getLastTrades(int)
	 */
	public List<TaggedTrade> getLastTradesOrderedIncludingSimulated(int n, String strategyId) {
		return exchangeClient.getLastTradesOrderedIncludingSimulated(n, strategyId);
	}

	/**
	 * @see ExchangeClient#addTradeListener(TaggedTradeListener)
	 */
	public void addTradeListener(TradeListener listener) {
		exchangeClient.addTradeListener(listener);
	}

	/**
	 * @see ExchangeClient#removeTradeListener(TaggedTradeListener)
	 */
	public void removeTradeListener(TradeListener listener) {
		exchangeClient.removeTradeListener(listener);
	}
}
