package net.rubenmartinez.stpc.exchange.bitso.orderbook;

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.domain.BitsoRestApiOrderBook;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.domain.BitsoOrder;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.dto.OrderBookBeanDTO;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.helper.SortedBookOrdersMap;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.helper.SortedBookOrdersMap.SortOrdering;

public class NewOrderBookSupplier implements Supplier<OrderBookBeanDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(NewOrderBookSupplier.class);
	private BitsoRestApiClient restClient;
	private String bookName;

	public NewOrderBookSupplier(BitsoRestApiClient restClient, String bookName) {
		this.restClient = restClient;
		this.bookName = bookName;
	}

	@Override
	public OrderBookBeanDTO get() {
		LOGGER.debug("Getting new full OrderBook");
		OrderBookBeanDTO orderBook = getNewFullBook();
		LOGGER.trace("OrderBook retrieved: {}", orderBook);
		return orderBook;
	}

	private OrderBookBeanDTO getNewFullBook() {
		BitsoRestApiOrderBook restApiOrderBook = restClient.getOrderBook(bookName, false);
		LOGGER.trace("restApiOrderBook: {}", restApiOrderBook);
		
		long currentSequence = restApiOrderBook.getPayload().getSequence();
		LOGGER.trace("New book retrieved. Resetting to sequence: {}", currentSequence);
		
		SortedBookOrdersMap asks = createNewOrdersMapFromRestOrders(restApiOrderBook.getPayload().getAsks(), SortOrdering.ASCENDING);
		SortedBookOrdersMap bids = createNewOrdersMapFromRestOrders(restApiOrderBook.getPayload().getBids(), SortOrdering.DESCENDING);
		return OrderBookBeanDTO.builder().sequence(currentSequence).asks(asks).bids(bids).build();
	}

	private static final SortedBookOrdersMap createNewOrdersMapFromRestOrders(List<BitsoRestApiOrderBook.Payload.Item> orders, SortOrdering ordering) {
		SortedBookOrdersMap ordersMap = new SortedBookOrdersMap(ordering);
		orders.stream().map(item -> new BitsoOrder(item.getOrderId(), item.getPrice(), item.getAmount())).forEach(ordersMap::put);
		return ordersMap;
	}
}
