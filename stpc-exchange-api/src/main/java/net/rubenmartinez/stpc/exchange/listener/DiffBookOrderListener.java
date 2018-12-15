package net.rubenmartinez.stpc.exchange.listener;

import net.rubenmartinez.stpc.exchange.domain.Order;
import net.rubenmartinez.stpc.exchange.domain.OrderBook;

public interface DiffBookOrderListener {
    
    /**
     * Called at first or when there is a book reset, 
     */
    public void onFullOrderBook(OrderBook orderBook);
    
    public void onDiffOrder(Order order);

}
