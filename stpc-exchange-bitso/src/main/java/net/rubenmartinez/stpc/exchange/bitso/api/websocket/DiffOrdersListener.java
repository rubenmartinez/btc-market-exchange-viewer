package net.rubenmartinez.stpc.exchange.bitso.api.websocket;

import net.rubenmartinez.stpc.exchange.bitso.api.websocket.domain.DiffOrdersWebsocketMessage;

public interface DiffOrdersListener {
	
	public void onDiffOrder(DiffOrdersWebsocketMessage message);
}
