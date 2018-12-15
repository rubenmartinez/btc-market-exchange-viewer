package net.rubenmartinez.stpc.exchange.bitso.orderbook;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient;
import net.rubenmartinez.stpc.exchange.bitso.api.websocket.domain.DiffOrdersWebsocketMessage;

/**
 * This class is not actually used in the project.
 * It was just a probe that actually a replayQueue is not really necessary because 
 * the websocket framework keeps already an internal queue of pending orders and it won't
 * call your listener method till it has finished (so till the book if reset if a re-reset is necessary)
 *
 */
public class SimpleOrderBookKeeper extends BaseOrderBookKeeper {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOrderBookKeeper.class);

	public SimpleOrderBookKeeper(BitsoRestApiClient client, String bookName) {
		super(client, bookName);
		
		reset();
		
		LOGGER.debug("Constructed");
	}
	
	@Override
	protected void reset() {
		LOGGER.info("Reset started");
		bookReadyForReadingLatch = new CountDownLatch(1);
		applyNewOrderBook(orderBookSupplier.get());
		bookReadyForReadingLatch.countDown();
		LOGGER.debug("Reset task finished");
	}
	
	/**
	 * This method cannot be called by two threads at the same time.
	 * Note that a websocket framework won't call the listener method concurrently so
	 * it is ok to call this method when receiving diffOrders from Bitso websocket.  
	 */
	@Override
	public void onDiffOrder(DiffOrdersWebsocketMessage diffOrdersMessage) {
		LOGGER.debug("Diff order received: {}", diffOrdersMessage);
		
		evaluateAndApplyDiffOrderMessage(diffOrdersMessage);
	}
}