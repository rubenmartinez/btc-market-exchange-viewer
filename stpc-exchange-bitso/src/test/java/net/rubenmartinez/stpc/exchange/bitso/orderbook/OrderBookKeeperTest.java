package net.rubenmartinez.stpc.exchange.bitso.orderbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Spy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.domain.BitsoRestApiOrderBook;
import net.rubenmartinez.stpc.exchange.bitso.api.websocket.domain.DiffOrdersWebsocketMessage;
import net.rubenmartinez.stpc.exchange.bitso.util.Resources;
import net.rubenmartinez.stpc.exchange.domain.Order;
import net.rubenmartinez.stpc.test.util.DelayedReturnAnswer;
import net.rubenmartinez.stpc.test.util.TestConfiguration;
import net.rubenmartinez.stpc.test.util.TestLoggingExtension;

@ExtendWith(TestLoggingExtension.class)
@ExtendWith(MockitoExtension.class)
public class OrderBookKeeperTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookKeeperTest.class);
	
	private static final String RESOURCES_DIR = "/OrderBookKeeperTest/";
	private static final String BOOK_RESOURCE_SEQ1 = RESOURCES_DIR + "restApiOrderBook3asks3bids.json";
	private static final String DIFF_ORDER_SEQ2_ADD2SELLS_ADD1BUY = RESOURCES_DIR + "diffOrdersSeq2Add2Sells1Buy.json";
	private static final String DIFF_ORDER_SEQ3_REMOVE1SELL_UPDATE1BUY = RESOURCES_DIR + "diffOrdersSeq3Remove1SellUpdate1Buy.json";
	private static final String DIFF_ORDER_REMOVE_NONEXISTENT_SEQ2 = RESOURCES_DIR + "diffOrdersRemoveNonexistentSeq2.json";
	private static final String DIFF_ORDER_IGNORE_REPEATED_SEQUENCE = RESOURCES_DIR + "diffOrdersIgnoreRepeatedSequence.json";
	private static final String DIFF_ORDER_DUPLICATED_PRICES_SEQ2 = RESOURCES_DIR + "diffOrdersSeq2DuplicatedPrices.json";
	private static final String BOOK_RESOURCE_SEQ5 = RESOURCES_DIR + "restApiOrderBook1asks1bidsSeq5.json";
	private static final String DIFF_ORDER_SEQ10_REMOVE1SELL = RESOURCES_DIR + "diffOrdersSeq10Remove1Sell.json";

	private static final String bookName = TestConfiguration.getTestBookName();
	
	@Spy
	private List<DiffOrdersWebsocketMessage> diffOrdersReplayQueue = new ArrayList<>();

	@SuppressWarnings("unused") // Used inside annotations @MethodSource actually
	private static Stream<BaseOrderBookKeeper> orderBookKeeperParams() {
		return Stream.of( // Note: Order of elements here is important. ReplayQueueOrderBookKeeper must be placed after SimpleOrderBookKeeper. Unfortunately JUnit5 (at least 5.3.1) consumes all the stream to be prepared before calling the test method, instead of consuming just one element per test method call. So as the creation of ReplayQueueOrderBookKeeper involves creating a new Thread that takes some seconds via mocking, these 2 seconds are "lost" for the tests while creating if creation of mocked SimpleOrderBookKeeper (also taking 2 seconds) was invoked before 
				new SimpleOrderBookKeeper(getMockedRestApiClient(Duration.ZERO), bookName),
				new SimpleOrderBookKeeper(getMockedRestApiClient(Duration.ofSeconds(2)), bookName),
				new ReplayQueueOrderBookKeeper(getMockedRestApiClient(Duration.ZERO), bookName),
				new ReplayQueueOrderBookKeeper(getMockedRestApiClient(Duration.ofSeconds(2)), bookName)
		);
	}
	
	private static BitsoRestApiClient getMockedRestApiClient(Duration delay) {
		BitsoRestApiClient mockedRestApiClient = mock(BitsoRestApiClient.class);
		BitsoRestApiOrderBook returnedBookSeq1 = Resources.getResourceAsParsedJson(BOOK_RESOURCE_SEQ1, BitsoRestApiOrderBook.class);
		BitsoRestApiOrderBook returnedBookSeq5 = Resources.getResourceAsParsedJson(BOOK_RESOURCE_SEQ5, BitsoRestApiOrderBook.class);
		BitsoRestApiOrderBook returnedBookSeq9 = Resources.getResourceAsParsedJson(BOOK_RESOURCE_SEQ1, BitsoRestApiOrderBook.class);
		returnedBookSeq9.getPayload().setSequence(9);

		when(mockedRestApiClient.getOrderBook(anyString(), eq(false)))
				.thenAnswer(new DelayedReturnAnswer<>(delay, returnedBookSeq1))
				.thenAnswer(new DelayedReturnAnswer<>(delay, returnedBookSeq5))
		        .thenAnswer(new DelayedReturnAnswer<>(delay, returnedBookSeq9));

		LOGGER.debug("Prepared mockedRestApiClient: {}", mockedRestApiClient);
		
		// Stubbed invocation should be enough without using verify, from mockito documentation:
		// Although it is possible to verify a stubbed invocation, usually it's just redundant
		// If your code cares what get(0) returns, then something else breaks (often even before verify() gets executed).
		// If your code doesn't care what get(0) returns, then it should not be stubbed. Not convinced? See http://monkeyisland.pl/2008/04/26/asking-and-telling
		return mockedRestApiClient;
	}
	
	@ParameterizedTest
	@MethodSource("orderBookKeeperParams")
	void orderBookInitialization(BaseOrderBookKeeper orderBookKeeper) {
		List<Order> asks = orderBookKeeper.getAsks(2);
		assertEquals(2, asks.size());
		assertEquals("101", asks.get(0).getPrice());
		assertEquals("102", asks.get(1).getPrice());
		asks = orderBookKeeper.getOrderBook().getAsks();
		assertEquals(3, asks.size());
		assertEquals("101", asks.get(0).getPrice());
		assertEquals("102", asks.get(1).getPrice());
		assertEquals("103", asks.get(2).getPrice());

		List<Order> bids = orderBookKeeper.getBids(10);
		assertEquals(3, bids.size());
		assertEquals("99", bids.get(0).getPrice());
		assertEquals("98", bids.get(1).getPrice());
		assertEquals("97", bids.get(2).getPrice());
		bids = orderBookKeeper.getOrderBook().getBids();
		assertEquals(3, bids.size());
		assertEquals("99", bids.get(0).getPrice());
		assertEquals("98", bids.get(1).getPrice());
		assertEquals("97", bids.get(2).getPrice());
	}

	@ParameterizedTest
	@MethodSource("orderBookKeeperParams")
	void duplicatedPrices(BaseOrderBookKeeper orderBookKeeper) {
		List<Order> asks = orderBookKeeper.getOrderBook().getAsks();
		assertEquals(3, asks.size());

		List<Order> bids = orderBookKeeper.getOrderBook().getBids();
		assertEquals(3, bids.size());

		DiffOrdersWebsocketMessage diffOrdersMessageSeq2 = Resources.getResourceAsParsedJson(DIFF_ORDER_DUPLICATED_PRICES_SEQ2, DiffOrdersWebsocketMessage.class);
		orderBookKeeper.onDiffOrder(diffOrdersMessageSeq2);

		asks = orderBookKeeper.getAsks(10);
		assertEquals(6, asks.size());
		assertEquals("101", asks.get(0).getPrice());
		assertEquals("101", asks.get(1).getPrice());
		assertEquals("102", asks.get(2).getPrice());
		assertEquals("102", asks.get(3).getPrice());
		assertEquals("103", asks.get(4).getPrice());
		assertEquals("103", asks.get(5).getPrice());

		bids = orderBookKeeper.getBids(10);
		assertEquals(6, bids.size());
		assertEquals("99", bids.get(0).getPrice());
		assertEquals("99", bids.get(1).getPrice());
		assertEquals("98", bids.get(2).getPrice());
		assertEquals("98", bids.get(3).getPrice());
		assertEquals("97", bids.get(4).getPrice());
		assertEquals("97", bids.get(5).getPrice());
	}
		
	@ParameterizedTest
	@MethodSource("orderBookKeeperParams")
	void diffOrdersSeveralSequencesCheckOrder(BaseOrderBookKeeper orderBookKeeper) {
		DiffOrdersWebsocketMessage diffOrdersMessageSeq2 = Resources.getResourceAsParsedJson(DIFF_ORDER_SEQ2_ADD2SELLS_ADD1BUY, DiffOrdersWebsocketMessage.class);
		orderBookKeeper.onDiffOrder(diffOrdersMessageSeq2);

		List<Order> asksSeq2 = orderBookKeeper.getAsks(10);
		assertEquals(5, asksSeq2.size());
		assertEquals("100.4", asksSeq2.get(0).getPrice());
		assertEquals("100.5", asksSeq2.get(1).getPrice());
		asksSeq2 = orderBookKeeper.getOrderBook().getAsks();
		assertEquals(5, asksSeq2.size());
		assertEquals("100.4", asksSeq2.get(0).getPrice());
		assertEquals("100.5", asksSeq2.get(1).getPrice());

		List<Order> bidsSeq2 = orderBookKeeper.getBids(3);
		assertEquals(3, bidsSeq2.size());
		assertEquals("99.7", bidsSeq2.get(0).getPrice());
		assertEquals("99", bidsSeq2.get(1).getPrice());
		assertEquals("2.23976", bidsSeq2.get(1).getAmount());
		bidsSeq2 = orderBookKeeper.getOrderBook().getBids();
		assertEquals(4, bidsSeq2.size());
		assertEquals("99.7", bidsSeq2.get(0).getPrice());
		assertEquals("99", bidsSeq2.get(1).getPrice());
		assertEquals("2.23976", bidsSeq2.get(1).getAmount());

		DiffOrdersWebsocketMessage diffOrdersMessageSeq3 = Resources.getResourceAsParsedJson(DIFF_ORDER_SEQ3_REMOVE1SELL_UPDATE1BUY, DiffOrdersWebsocketMessage.class);
		orderBookKeeper.onDiffOrder(diffOrdersMessageSeq3);

		List<Order> asksSeq3 = orderBookKeeper.getAsks(10);
		assertEquals(4, asksSeq3.size());
		assertEquals("100.4", asksSeq3.get(0).getPrice());
		assertEquals("101", asksSeq3.get(1).getPrice());
		asksSeq3 = orderBookKeeper.getOrderBook().getAsks();
		assertEquals(4, asksSeq3.size());
		assertEquals("100.4", asksSeq3.get(0).getPrice());
		assertEquals("101", asksSeq3.get(1).getPrice());

		List<Order> bidsSeq3 = orderBookKeeper.getBids(3);
		assertEquals(3, bidsSeq3.size());
		assertEquals("99.7", bidsSeq3.get(0).getPrice());
		assertEquals("99", bidsSeq3.get(1).getPrice());
		assertEquals("0.5", bidsSeq3.get(1).getAmount());
		bidsSeq3 = orderBookKeeper.getOrderBook().getBids();
		assertEquals(4, bidsSeq3.size());
		assertEquals("99.7", bidsSeq3.get(0).getPrice());
		assertEquals("99", bidsSeq3.get(1).getPrice());
		assertEquals("0.5", bidsSeq3.get(1).getAmount());
	}
	
	@Test
	void twoMessagesInReplyQueue() {
		ReplayQueueOrderBookKeeper orderBookKeeperSpied = new ReplayQueueOrderBookKeeper(getMockedRestApiClient(Duration.ofSeconds(2)), bookName);
		orderBookKeeperSpied.diffOrdersReplayQueue = diffOrdersReplayQueue; // Cannot use @InjectMocks because it will initialize the thread too soon (while other tests are running)
				
		DiffOrdersWebsocketMessage diffOrdersMessageSeq2 = Resources.getResourceAsParsedJson(DIFF_ORDER_SEQ2_ADD2SELLS_ADD1BUY, DiffOrdersWebsocketMessage.class);
		orderBookKeeperSpied.onDiffOrder(diffOrdersMessageSeq2);

	    verify(diffOrdersReplayQueue).add(diffOrdersMessageSeq2);
		
		DiffOrdersWebsocketMessage diffOrdersMessageSeq3 = Resources.getResourceAsParsedJson(DIFF_ORDER_SEQ3_REMOVE1SELL_UPDATE1BUY, DiffOrdersWebsocketMessage.class);
		orderBookKeeperSpied.onDiffOrder(diffOrdersMessageSeq3);

		verify(diffOrdersReplayQueue).add(diffOrdersMessageSeq3);
	    assertEquals(2, diffOrdersReplayQueue.size());		
		
		List<Order> asksSeq3 = orderBookKeeperSpied.getAsks(10);
		assertEquals(4, asksSeq3.size());
		assertEquals("100.4", asksSeq3.get(0).getPrice());
		assertEquals("101", asksSeq3.get(1).getPrice());
		asksSeq3 = orderBookKeeperSpied.getOrderBook().getAsks();
		assertEquals(4, asksSeq3.size());
		assertEquals("100.4", asksSeq3.get(0).getPrice());
		assertEquals("101", asksSeq3.get(1).getPrice());

		List<Order> bidsSeq3 = orderBookKeeperSpied.getBids(3);
		assertEquals(3, bidsSeq3.size());
		assertEquals("99.7", bidsSeq3.get(0).getPrice());
		assertEquals("99", bidsSeq3.get(1).getPrice());
		assertEquals("0.5", bidsSeq3.get(1).getAmount());
		bidsSeq3 = orderBookKeeperSpied.getOrderBook().getBids();
		assertEquals(4, bidsSeq3.size());
		assertEquals("99.7", bidsSeq3.get(0).getPrice());
		assertEquals("99", bidsSeq3.get(1).getPrice());
		assertEquals("0.5", bidsSeq3.get(1).getAmount());
		
	    assertEquals(0, diffOrdersReplayQueue.size());		
	}	
	
	@ParameterizedTest
	@MethodSource("orderBookKeeperParams")
	void ignoreRemoveUnexistent(BaseOrderBookKeeper orderBookKeeper) throws InterruptedException {
		DiffOrdersWebsocketMessage diffOrdersMessageSeq2 = Resources.getResourceAsParsedJson(DIFF_ORDER_REMOVE_NONEXISTENT_SEQ2, DiffOrdersWebsocketMessage.class);
		orderBookKeeper.onDiffOrder(diffOrdersMessageSeq2);

		List<Order> asks = orderBookKeeper.getAsks(2);
		assertEquals(2, asks.size());
		assertEquals("101", asks.get(0).getPrice());
		assertEquals("102", asks.get(1).getPrice());
		asks = orderBookKeeper.getOrderBook().getAsks();
		assertEquals(3, asks.size());
		assertEquals("101", asks.get(0).getPrice());
		assertEquals("102", asks.get(1).getPrice());
		assertEquals("103", asks.get(2).getPrice());

		List<Order> bids = orderBookKeeper.getBids(10);
		assertEquals(3, bids.size());
		assertEquals("99", bids.get(0).getPrice());
		assertEquals("98", bids.get(1).getPrice());
		assertEquals("97", bids.get(2).getPrice());
		bids = orderBookKeeper.getOrderBook().getBids();
		assertEquals(3, bids.size());
		assertEquals("99", bids.get(0).getPrice());
		assertEquals("98", bids.get(1).getPrice());
		assertEquals("97", bids.get(2).getPrice());

		TimeUnit.SECONDS.sleep(1L);

		diffOrdersMessageSeq2 = Resources.getResourceAsParsedJson(DIFF_ORDER_REMOVE_NONEXISTENT_SEQ2, DiffOrdersWebsocketMessage.class);
		orderBookKeeper.onDiffOrder(diffOrdersMessageSeq2);

		asks = orderBookKeeper.getAsks(2);
		assertEquals(2, asks.size());
		assertEquals("101", asks.get(0).getPrice());
		assertEquals("102", asks.get(1).getPrice());
		asks = orderBookKeeper.getOrderBook().getAsks();
		assertEquals(3, asks.size());
		assertEquals("101", asks.get(0).getPrice());
		assertEquals("102", asks.get(1).getPrice());
		assertEquals("103", asks.get(2).getPrice());

		bids = orderBookKeeper.getBids(10);
		assertEquals(3, bids.size());
		assertEquals("99", bids.get(0).getPrice());
		assertEquals("98", bids.get(1).getPrice());
		assertEquals("97", bids.get(2).getPrice());
		bids = orderBookKeeper.getOrderBook().getBids();
		assertEquals(3, bids.size());
		assertEquals("99", bids.get(0).getPrice());
		assertEquals("98", bids.get(1).getPrice());
		assertEquals("97", bids.get(2).getPrice());
	}

	@ParameterizedTest
	@MethodSource("orderBookKeeperParams")
	void ignoreRepeatedSequence(BaseOrderBookKeeper orderBookKeeper) {
		DiffOrdersWebsocketMessage diffOrdersMessageSeq1 = Resources.getResourceAsParsedJson(DIFF_ORDER_IGNORE_REPEATED_SEQUENCE, DiffOrdersWebsocketMessage.class);
		orderBookKeeper.onDiffOrder(diffOrdersMessageSeq1);

		List<Order> asks = orderBookKeeper.getAsks(2);
		assertEquals(2, asks.size());
		assertEquals("101", asks.get(0).getPrice());
		assertEquals("102", asks.get(1).getPrice());

		List<Order> bids = orderBookKeeper.getBids(10);

		assertEquals(3, bids.size());
		assertEquals("99", bids.get(0).getPrice());
		assertEquals("98", bids.get(1).getPrice());
		assertEquals("97", bids.get(2).getPrice());

		DiffOrdersWebsocketMessage diffOrdersMessageSeq2 = Resources.getResourceAsParsedJson(DIFF_ORDER_SEQ2_ADD2SELLS_ADD1BUY, DiffOrdersWebsocketMessage.class);
		orderBookKeeper.onDiffOrder(diffOrdersMessageSeq2);

		List<Order> asksSeq2 = orderBookKeeper.getAsks(10);
		assertEquals(5, asksSeq2.size());
		assertEquals("100.4", asksSeq2.get(0).getPrice());
		assertEquals("100.5", asksSeq2.get(1).getPrice());

		List<Order> bidsSeq2 = orderBookKeeper.getBids(3);
		assertEquals(3, bidsSeq2.size());
		assertEquals("99.7", bidsSeq2.get(0).getPrice());
		assertEquals("99", bidsSeq2.get(1).getPrice());
		assertEquals("2.23976", bidsSeq2.get(1).getAmount());

		diffOrdersMessageSeq2 = Resources.getResourceAsParsedJson(DIFF_ORDER_IGNORE_REPEATED_SEQUENCE, DiffOrdersWebsocketMessage.class);
		orderBookKeeper.onDiffOrder(diffOrdersMessageSeq2);

		asksSeq2 = orderBookKeeper.getAsks(10);
		assertEquals(5, asksSeq2.size());
		assertEquals("100.4", asksSeq2.get(0).getPrice());
		assertEquals("100.5", asksSeq2.get(1).getPrice());

		bidsSeq2 = orderBookKeeper.getBids(3);
		assertEquals(3, bidsSeq2.size());
		assertEquals("99.7", bidsSeq2.get(0).getPrice());
		assertEquals("99", bidsSeq2.get(1).getPrice());
		assertEquals("2.23976", bidsSeq2.get(1).getAmount());
	}

	@ParameterizedTest
	@MethodSource("orderBookKeeperParams")
	void resetOnMissingSequence(BaseOrderBookKeeper orderBookKeeper) {
		DiffOrdersWebsocketMessage diffOrdersMessage = Resources.getResourceAsParsedJson(DIFF_ORDER_SEQ3_REMOVE1SELL_UPDATE1BUY, DiffOrdersWebsocketMessage.class);
		orderBookKeeper.onDiffOrder(diffOrdersMessage);
		
		// BOOK_RESOURCE_SEQ5 should be retrieved now

		List<Order> asks = orderBookKeeper.getAsks(10);
		assertEquals(1, asks.size());
		assertEquals("100.01", asks.get(0).getPrice());
		asks = orderBookKeeper.getOrderBook().getAsks();
		assertEquals(1, asks.size());
		assertEquals("100.01", asks.get(0).getPrice());

		List<Order> bids = orderBookKeeper.getBids(10);
		assertEquals(1, bids.size());
		assertEquals("99.99", bids.get(0).getPrice());
		bids = orderBookKeeper.getOrderBook().getBids();
		assertEquals(1, bids.size());
		assertEquals("99.99", bids.get(0).getPrice());
		
		diffOrdersMessage.setSequence(8);
		orderBookKeeper.onDiffOrder(diffOrdersMessage);

		// As instructed in method getMockedRestApiClient, the book with sequence 9 is returned now (which is identical to the initial one, just the sequence changed)	
		
		asks = orderBookKeeper.getAsks(2);
		assertEquals(2, asks.size());
		assertEquals("101", asks.get(0).getPrice());
		assertEquals("102", asks.get(1).getPrice());
		asks = orderBookKeeper.getOrderBook().getAsks();
		assertEquals(3, asks.size());
		assertEquals("101", asks.get(0).getPrice());
		assertEquals("102", asks.get(1).getPrice());
		assertEquals("103", asks.get(2).getPrice());

		bids = orderBookKeeper.getBids(10);
		assertEquals(3, bids.size());
		assertEquals("99", bids.get(0).getPrice());
		assertEquals("98", bids.get(1).getPrice());
		assertEquals("97", bids.get(2).getPrice());
		bids = orderBookKeeper.getOrderBook().getBids();
		assertEquals(3, bids.size());
		assertEquals("99", bids.get(0).getPrice());
		assertEquals("98", bids.get(1).getPrice());
		assertEquals("97", bids.get(2).getPrice());

		diffOrdersMessage = Resources.getResourceAsParsedJson(DIFF_ORDER_SEQ10_REMOVE1SELL, DiffOrdersWebsocketMessage.class);
		orderBookKeeper.onDiffOrder(diffOrdersMessage);
		
		// Finally sequence is correct and one sell order is removed
		asks = orderBookKeeper.getOrderBook().getAsks();
		assertEquals(2, asks.size());
	}

}
