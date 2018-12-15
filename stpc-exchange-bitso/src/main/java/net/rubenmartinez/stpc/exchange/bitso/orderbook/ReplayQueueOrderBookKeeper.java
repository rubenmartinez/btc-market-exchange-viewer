package net.rubenmartinez.stpc.exchange.bitso.orderbook;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient;
import net.rubenmartinez.stpc.exchange.bitso.api.websocket.domain.DiffOrdersWebsocketMessage;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.exception.SequenceMissingAgainWhileResettingException;

/**
 *
 */
public class ReplayQueueOrderBookKeeper extends BaseOrderBookKeeper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReplayQueueOrderBookKeeper.class);
	
	private static final int LOG_ERROR_AFTER_CONSECUTIVE_TRIES_RESETING = 10;
	
	private ExecutorService resetBookExecutor; 
	private ResetBookTask resetBookTask;
	private volatile boolean resetting;
	
	@Resource
	List<DiffOrdersWebsocketMessage> diffOrdersReplayQueue;

	public ReplayQueueOrderBookKeeper(BitsoRestApiClient client, String bookName) {
		super(client, bookName);
		
		this.diffOrdersReplayQueue = new ArrayList<>();
		this.resetBookTask = new ResetBookTask();
		this.resetBookExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "ResetBookExec"));
		
		reset();

		LOGGER.debug("Constructed");
	}
	
	@Override
	protected void reset() {
		LOGGER.info("Reset started");
		bookReadyForReadingLatch = new CountDownLatch(1);
		resetting = true;
		
		resetBookExecutor.execute(resetBookTask);
		
		LOGGER.debug("Reset task scheduled");
	}
	
	
	/**
	 * This method cannot be called by two threads at the same time.
	 * Note that a websocket framework won't call the listener method concurrently so
	 * it is ok to call this method when receiving diffOrders from Bitso websocket.  
	 */
	@Override
	public void onDiffOrder(DiffOrdersWebsocketMessage diffOrdersMessage) {
		LOGGER.debug("Diff order received: {}", diffOrdersMessage);

		if (resetting) {
			synchronized (diffOrdersReplayQueue) {
				if (resetting) { // double-checked-locking is fine with a boolean volatile variable. Thanks to this there is no need for locking in normal functioning (without reply queue)
					LOGGER.debug("Book is being reset, queuing diffOrderMessage to apply it later");
					diffOrdersReplayQueue.add(diffOrdersMessage);
				}
			}
		}
		else {
			evaluateAndApplyDiffOrderMessage(diffOrdersMessage);
		}
		
	}
	
	
	private class ResetBookTask implements Runnable {
		
		@Override
		public void run() {
			int tryNumber = 0;
			do {
				try {
					applyNewOrderBook(orderBookSupplier.get());
					
					// Once a new OrderBook is retrieved and set, we apply the orders that could have arrive meanwhile during resetting
					synchronized (diffOrdersReplayQueue) {
						LOGGER.debug("Replaying {} DiffOrdersWebsocketMessage(s)", diffOrdersReplayQueue.size());

						diffOrdersReplayQueue.stream()
							.filter(message -> message.getSequence() > currentSequence)
							.forEach(this::replayMessage);

						diffOrdersReplayQueue.clear();
						LOGGER.debug("DiffOrdersWebsocketMessage reply queue applied");	
						
						resetting = false;
					}
					
					bookReadyForReadingLatch.countDown();
					LOGGER.info("OrderBook reset completed. Sequence: {}; Number of Asks: {}; Number of Bids: {}", currentSequence, asks.size(), bids.size());

				} catch (Exception e) {
					tryNumber++;
					manageResetBookException(e, tryNumber);
				}
			} while (resetting);
		}

		private void replayMessage(DiffOrdersWebsocketMessage message) {
			if (message.getSequence() == (currentSequence + 1)) {
				message.getPayload().forEach(diffOrder -> applyDiffOrder(diffOrder));
				currentSequence++;
			} else {
				LOGGER.warn("Sequence lost while *replaying*. Message sequence: [{}], current sequence: [{}]", message.getSequence(), currentSequence);
				throw new SequenceMissingAgainWhileResettingException("Sequence lost while *replaying*. Message sequence: ["+message.getSequence()+"], current sequence: ["+currentSequence+"]");
			}
		}

		private void manageResetBookException(Exception e, int tryNumber) {
			// There is no much more to do here than retry again and again while trace errors. An alternative could be throwing a java.lang.Error to stop the application after N retries but that would be too much
			// Note the bookReadyForReadingLatch.await uses a timeout so the clients will be informed anyway if resetting is taking too long 
			if (tryNumber >= LOG_ERROR_AFTER_CONSECUTIVE_TRIES_RESETING) {
				LOGGER.error("More than " + LOG_ERROR_AFTER_CONSECUTIVE_TRIES_RESETING + " consecutive exceptions while resetting OrderBook. Retrying (#" + tryNumber + ")", e);
			}
			else {
				LOGGER.warn("Exception while resetting OrderBook. Retrying (#{})", tryNumber);
			}
		}

	}
}