package net.rubenmartinez.stpc.exchange.bitso.orderbook;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubenmartinez.stpc.exchange.bitso.BitsoExchangeThreadInterruptedException;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient;
import net.rubenmartinez.stpc.exchange.bitso.api.websocket.DiffOrdersListener;
import net.rubenmartinez.stpc.exchange.bitso.api.websocket.domain.DiffOrdersWebsocketMessage;
import net.rubenmartinez.stpc.exchange.bitso.configuration.Configuration;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.dto.OrderBookBeanDTO;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.domain.BitsoOrder;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.domain.BitsoOrderBook;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.exception.OrderBookResetTimeOutException;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.helper.SortedBookOrdersMap;
import net.rubenmartinez.stpc.exchange.domain.Order;
import net.rubenmartinez.stpc.exchange.domain.OrderBook;

public abstract class BaseOrderBookKeeper implements DiffOrdersListener, OrderBookKeeper {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOrderBookKeeper.class);

	protected static final long SEQUENCE_NOT_INITIZALIZED = -1;

	protected String bookName;

	protected volatile SortedBookOrdersMap asks;
	protected volatile SortedBookOrdersMap bids;
	protected volatile long currentSequence;

	protected volatile CountDownLatch bookReadyForReadingLatch;

	protected NewOrderBookSupplier orderBookSupplier;

	public BaseOrderBookKeeper(BitsoRestApiClient client, String bookName) {
		this.bookName = bookName;
		this.currentSequence = SEQUENCE_NOT_INITIZALIZED;
		this.orderBookSupplier = new NewOrderBookSupplier(client, bookName);

		LOGGER.debug("Constructed");
	}

	/**
	 * 
	 */
	protected abstract void reset();

	protected void evaluateAndApplyDiffOrderMessage(DiffOrdersWebsocketMessage diffOrdersMessage) {
		if (diffOrdersMessage.getSequence() < (currentSequence + 1)) {
			LOGGER.warn("Websocket diff-order, ignoring repeated message, sequence: {}, current sequence is: {}", diffOrdersMessage.getSequence(), currentSequence);
		} else {
			if (diffOrdersMessage.getSequence() == (currentSequence + 1)) {
				diffOrdersMessage.getPayload().forEach(this::applyDiffOrder);
				currentSequence++;
			} else {
				LOGGER.warn("RESET NEEDED. Websocket diff-order received with a non-consecutive sequence: {}, current sequence was: {}", diffOrdersMessage.getSequence(),
						currentSequence);
				reset();

			}
		}
	}

	protected void applyDiffOrder(DiffOrdersWebsocketMessage.DiffOrder diffOrder) {
		switch (diffOrder.getOrderType()) {
		case SELL:
			applyDiffOrderToASortedOrdersMap(this.asks, diffOrder);
			break;
		case BUY:
			applyDiffOrderToASortedOrdersMap(this.bids, diffOrder);
			break;
		default:
			throw new IllegalArgumentException("Unexpected order type: " + diffOrder.getOrderType());
		}
	}

	private static void applyDiffOrderToASortedOrdersMap(SortedBookOrdersMap sortedOrdersMap, DiffOrdersWebsocketMessage.DiffOrder diffOrderMessage) {
		String orderId = diffOrderMessage.getId();
		String amount = diffOrderMessage.getAmount();

		if (amount == null || amount.equals("") || amount.equals("0")) {
			if (amount != null) {
				LOGGER.warn(
						"Amount was present in message but is empty or zero. Assuming order should be removed, even if Bitso spec specifies that property amount won't be present in this case");
			}
			Optional<BitsoOrder> previousOrder = sortedOrdersMap.remove(orderId);
			if (previousOrder.isPresent()) {
				LOGGER.debug("Removed order from orderbook: {}; from diffOrder: {}", previousOrder, diffOrderMessage);
			} else {
				LOGGER.warn("Order didn't exist in orderbook: {}", orderId);
			}
		} else {
			Optional<BitsoOrder> existingOrder = sortedOrdersMap.get(orderId);
			if (existingOrder.isPresent()) {
				// orderList.put could be used also for updating, but this else-block is just to
				// avoid creating a new order as it is unnecessary. Plus, debug log trace is
				// more descriptive
				existingOrder.get().setAmount(amount);
				LOGGER.debug("Updated order: {}; from diffOrder: {}", existingOrder, diffOrderMessage);
			} else {
				BitsoOrder newOrder = new BitsoOrder(orderId, diffOrderMessage.getRate(), amount);
				sortedOrdersMap.put(newOrder);
				LOGGER.debug("Added new order to orderbook: {}; from diffOrder: {}", newOrder, diffOrderMessage);
			}
		}
	}

	protected void applyNewOrderBook(OrderBookBeanDTO orderBookBean) {
		LOGGER.debug("Applying new orderbook with sequence: {}", orderBookBean.getSequence());

		asks = orderBookBean.getAsks();
		bids = orderBookBean.getBids();
		currentSequence = orderBookBean.getSequence();

	}

	/**
	 * Returns a copy of
	 * 
	 */
	public OrderBook getOrderBook() {
		checkBookReady();

		BitsoOrderBook orderBook = new BitsoOrderBook();
		orderBook.setPair(this.bookName);
		orderBook.setAsks(this.asks.getSortedOrdersThreadSafe());
		orderBook.setBids(this.bids.getSortedOrdersThreadSafe());

		return orderBook;
	}

	public List<Order> getAsks(int n) {
		checkBookReady();
		return asks.getBestNSortedOrdersThreadSafe(n);
	}

	/**
	 * 
	 * 
	 * In the case the thread is interrupted while waiting for book ready, this will
	 * return null
	 */
	public List<Order> getBids(int n) {
		checkBookReady();
		return bids.getBestNSortedOrdersThreadSafe(n);
	}

	/**
	 * If book is currently resetting (either at startup time or after sequence is
	 * lost) this method will wait till the book is finally ready for reading
	 */
	private final void checkBookReady() {
		try {
			LOGGER.trace("Checking book ready");
			boolean ready = bookReadyForReadingLatch.await(Configuration.getOrderBookReadyTimeoutSeconds(), TimeUnit.SECONDS);
			if (!ready) {
				throw new OrderBookResetTimeOutException("OrderBook not ready after waiting for " + Configuration.getOrderBookReadyTimeoutSeconds() + " seconds.");
			}
			LOGGER.trace("Book ready");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BitsoExchangeThreadInterruptedException("Interrupted while waiting for book ready. Aborting operation");
		}

	}
}
