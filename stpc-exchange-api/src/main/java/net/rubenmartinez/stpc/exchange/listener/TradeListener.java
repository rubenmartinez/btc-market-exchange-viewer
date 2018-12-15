package net.rubenmartinez.stpc.exchange.listener;

import net.rubenmartinez.stpc.exchange.domain.Trade;

/**
 * 
 *
 */
public interface TradeListener {
    
    public void onNewTrade(Trade trade);
}
