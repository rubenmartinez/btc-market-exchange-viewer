package net.rubenmartinez.stpc.app.exchange.service.listener;

import net.rubenmartinez.stpc.app.exchange.domain.TaggedTrade;

/**
 * 
 *
 */
public interface TaggedTradeListener {
    
    public void onNewTrade(TaggedTrade trade);
}
