package net.rubenmartinez.stpc.exchange.domain;

import java.util.List;

/**
 * A copy of a Exchange's OrderBook at a point[event] of time
 * 
 */
public interface OrderBook {
	public String getPair();
	
	public List<Order> getAsks();
	public List<Order> getBids();
}
