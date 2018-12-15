package net.rubenmartinez.stpc.exchange.bitso.trade;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.BitsoRestApiClient.TradesSort;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.domain.BitsoRestApiTrades;
import net.rubenmartinez.stpc.exchange.bitso.configuration.Configuration;
import net.rubenmartinez.stpc.exchange.bitso.orderbook.OrderBookKeeperTest;
import net.rubenmartinez.stpc.exchange.bitso.trade.helper.NewTradesNotifier;
import net.rubenmartinez.stpc.exchange.bitso.util.Resources;
import net.rubenmartinez.stpc.exchange.domain.Trade;
import net.rubenmartinez.stpc.exchange.listener.TradeListener;
import net.rubenmartinez.stpc.test.util.TestConfiguration;
import net.rubenmartinez.stpc.test.util.TestLoggingExtension;
import net.rubenmartinez.stpc.test.util.TestingTrade;

@ExtendWith(TestLoggingExtension.class)
public class TradesHolderTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookKeeperTest.class);
	
	private static final String RESOURCES_DIR = "/TradesHolderTest/";
	private static final String NEWEST_TRADE_REST = RESOURCES_DIR + "newestTrade-rest.json";
	private static final String NEWEST_TRADE_STPC_API = RESOURCES_DIR + "newestTrade-stpc-api.json";
	private static final String SECOND_CALL_REST = RESOURCES_DIR + "secondCall-rest.json";
	private static final String SECOND_CALL_STPC_API_1 = RESOURCES_DIR + "secondCall-stpc-api-1.json";
	private static final String SECOND_CALL_STPC_API_2 = RESOURCES_DIR + "secondCall-stpc-api-2.json";
	private static final String THIRD_CALL_REST_OLDER = RESOURCES_DIR + "thirdCall-rest_older.json";
	private static final String THIRD_CALL_REST_OLDER_STPC_API = RESOURCES_DIR + "thirdCall-rest_older-stpc-api.json";

	private static final String bookName = TestConfiguration.getTestBookName();
	
	TradesHolder tradesHolder;
	TradeListener tradeListener;
	
	@BeforeEach
	void beforeEach() {
		tradesHolder = new TradesHolder(getMockedRestApiClient(), bookName);
		tradeListener = mock(TradeListener.class);
		tradesHolder.addTradeListener(tradeListener);
		tradesHolder.start();
	}
	
	private static BitsoRestApiClient getMockedRestApiClient() {
		BitsoRestApiClient mockedRestApiClient = mock(BitsoRestApiClient.class);
		BitsoRestApiTrades newestTrade = Resources.getResourceAsParsedJson(NEWEST_TRADE_REST, BitsoRestApiTrades.class);
		BitsoRestApiTrades secondCall = Resources.getResourceAsParsedJson(SECOND_CALL_REST, BitsoRestApiTrades.class);
		BitsoRestApiTrades thirdCallOlder = Resources.getResourceAsParsedJson(THIRD_CALL_REST_OLDER, BitsoRestApiTrades.class);
		
		when(mockedRestApiClient.getNewestTrades(anyString(), eq(1))).thenReturn(newestTrade);
		when(mockedRestApiClient.getTrades(anyString(), anyString(), eq(TradesSort.ASC), anyInt())).thenReturn(secondCall);
		when(mockedRestApiClient.getTrades(anyString(), anyString(), eq(TradesSort.DESC), anyInt())).thenReturn(thirdCallOlder);

		LOGGER.debug("Prepared mockedRestApiClient: {}", mockedRestApiClient);
		
		return mockedRestApiClient;
	}
	
	@Test
	void listenerAndHolder() {
		Trade firstTrade = Resources.getResourceAsParsedJson(NEWEST_TRADE_STPC_API, TestingTrade.class);
		verify(tradeListener).onNewTrade(eq(firstTrade));
		assertEquals(firstTrade, tradesHolder.getLastTrades(1).get(0));
		
		LOGGER.info("Sleeping till next trade poll...");
		sleepSeconds(Configuration.getTradeHolderPollSeconds() + 2);
		
		Trade secondTrade = Resources.getResourceAsParsedJson(SECOND_CALL_STPC_API_1, TestingTrade.class);
		Trade thirdTrade = Resources.getResourceAsParsedJson(SECOND_CALL_STPC_API_2, TestingTrade.class);
		
		verify(tradeListener).onNewTrade(eq(secondTrade));
		verify(tradeListener).onNewTrade(eq(thirdTrade));
		
		assertEquals(thirdTrade, tradesHolder.getLastTrades(3).get(0));
		assertEquals(secondTrade, tradesHolder.getLastTrades(3).get(1));
		assertEquals(firstTrade, tradesHolder.getLastTrades(3).get(2));

		Trade oldestTrade = Resources.getResourceAsParsedJson(THIRD_CALL_REST_OLDER_STPC_API, TestingTrade.class);
		
		assertEquals(thirdTrade, tradesHolder.getLastTrades(4).get(0));
		assertEquals(secondTrade, tradesHolder.getLastTrades(4).get(1));
		assertEquals(firstTrade, tradesHolder.getLastTrades(4).get(2));
		assertEquals(oldestTrade, tradesHolder.getLastTrades(4).get(3));
		
	}

	
	private static void sleepSeconds(int seconds) {
		// This should be substituted: Tests could fail under some circumstances as they have some assumptions on async code durations, even if the "timeouts" should be high enough, on slow machines a timeout for an async code could trigger so the test is assumed to fail, even if the code is working correctly.
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			LOGGER.warn("Sleep interrupted");
			Thread.currentThread().interrupt();
		}
	}			
	
}
