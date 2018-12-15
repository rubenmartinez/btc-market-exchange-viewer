package net.rubenmartinez.stpc.exchange;

import java.math.BigDecimal;
import java.util.List;

import net.rubenmartinez.stpc.exchange.domain.Order;
import net.rubenmartinez.stpc.exchange.domain.OrderBook;
import net.rubenmartinez.stpc.exchange.domain.OrderSide;
import net.rubenmartinez.stpc.exchange.domain.Trade;
import net.rubenmartinez.stpc.exchange.listener.TradeListener;

/**
 * For a specific book (pair)
 * 
 */
public interface ExchangeClient {
	
	/**
	 * Returns id
	 * 
	 * @param side
	 * @param price
	 * @param amount
	 * @return
	 */
    public String placeLimitOrder(OrderSide side, BigDecimal price, BigDecimal amount);
	
    /**
     */
	public OrderBook getOrderBook();
	
	/**
	 * Returns as much as n bids from the current OrderBook
	 * 
	 * Using this method instead of {@link ExchangeClient#getOrderBook()} might reduce thread blocking a bit
	 * 
	 * @param n maximum amount of bids to be returned
	 * 
	 * @return A list with as much as {@code n} bids from current OrderBook
	 */
	public List<Order> getBids(int n);

	/**
	 * 
	 */
	public List<Order> getAsks(int n);
	
	
	/**
	 * Get Last <code>n</code> trades ordered from the most recent trade to oldest.
	 * That is, the element on position [0] is the most recent in time.
	 * 
	 * @param bookName The book name
	 * @param n number of trades to receive
	 * @return last <code>n</code> trades ordered from the most recent trade to oldest
	 */
	public List<Trade> getLastTrades(int n);
	
	/**
	 * 
	 * @param listener
	 */
	public void addTradeListener(TradeListener listener);
	
	public void removeTradeListener(TradeListener listener);
}
