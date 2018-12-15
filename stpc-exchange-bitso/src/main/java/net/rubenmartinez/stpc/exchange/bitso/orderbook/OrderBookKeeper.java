package net.rubenmartinez.stpc.exchange.bitso.orderbook;

import java.util.List;

import net.rubenmartinez.stpc.exchange.domain.Order;
import net.rubenmartinez.stpc.exchange.domain.OrderBook;

public interface OrderBookKeeper {
    
    public OrderBook getOrderBook();

    public List<Order> getBids(int n);
    
    public List<Order> getAsks(int n);
    
}