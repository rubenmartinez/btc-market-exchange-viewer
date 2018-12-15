package net.rubenmartinez.stpc.app.strategy.implementations.contrarian;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.rubenmartinez.stpc.app.exchange.decorator.SimulatedTradesExchangeClient;
import net.rubenmartinez.stpc.app.exchange.domain.TaggedTrade;
import net.rubenmartinez.stpc.exchange.domain.OrderSide;
import net.rubenmartinez.stpc.exchange.domain.Trade;

public class ContrarianStrategyTest {
	private static final int CONSECUTIVE_UPTICKS_TO_SELL = 3;
	private static final int CONSECUTIVE_DOWNTICKS_TO_BUY = 2;

	private SimulatedTradesExchangeClient mockedSimulatedTradesExchangeClient;

	private ContrarianStrategy contrarianStrategy;

	private static SimulatedTradesExchangeClient getMockedExchangeClient() {
		SimulatedTradesExchangeClient exchangeClient = mock(SimulatedTradesExchangeClient.class);

		ArrayList<Trade> lastTrades = new ArrayList<>();
		lastTrades.add(new TaggedTrade("initialLastTrade", OrderSide.SELL, new BigDecimal("74000"), new BigDecimal("0.01")));
		when(exchangeClient.getLastTrades(1)).thenReturn(lastTrades);
		
		return exchangeClient;
	}
	
	private static ContrarianStrategyConfig getContrarianStrategyConfig() {
		ContrarianStrategyConfig contrarianStrategyConfig = new ContrarianStrategyConfig();
		contrarianStrategyConfig.setConsecutiveUpticksToSell(CONSECUTIVE_UPTICKS_TO_SELL);
		contrarianStrategyConfig.setConsecutiveDownticksToBuy(CONSECUTIVE_DOWNTICKS_TO_BUY);
		contrarianStrategyConfig.setTradeAmountInBaseCurrency(BigDecimal.ONE);
		contrarianStrategyConfig.setResetTicksAfterTrade(true);
		contrarianStrategyConfig.setPastTradeToEvaluate(1);
		
		return contrarianStrategyConfig;
	}
	
	@Before
	public void before() {
		mockedSimulatedTradesExchangeClient = getMockedExchangeClient();
		contrarianStrategy = new ContrarianStrategy("contrarian1", getContrarianStrategyConfig(), mockedSimulatedTradesExchangeClient);
		contrarianStrategy.activate();
	}

	@After
	public void after() {
		contrarianStrategy.deactivate();
	}

	@Test
	public void sellAfterConsecutiveUpticks() {
		TaggedTrade trade;
		trade = new TaggedTrade("test1", OrderSide.BUY, new BigDecimal("74010"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		trade = new TaggedTrade("test2", OrderSide.BUY, new BigDecimal("74020"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		trade = new TaggedTrade("test3", OrderSide.BUY, new BigDecimal("74030"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("74030")), eq(BigDecimal.ONE), any());
		
		trade = new TaggedTrade("test4", OrderSide.BUY, new BigDecimal("74040"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		trade = new TaggedTrade("test5", OrderSide.BUY, new BigDecimal("74050"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		trade = new TaggedTrade("test5", OrderSide.BUY, new BigDecimal("74060"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(2)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("74060")), eq(BigDecimal.ONE), any());
	}

	@Test
	public void buyAfterConsecutiveDownticks() {
		TaggedTrade trade;
		trade = new TaggedTrade("test1", OrderSide.SELL, new BigDecimal("73990"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		trade = new TaggedTrade("test2", OrderSide.SELL, new BigDecimal("73980"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("73980")), eq(BigDecimal.ONE), any());
		
		trade = new TaggedTrade("test3", OrderSide.SELL, new BigDecimal("73970"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
		trade = new TaggedTrade("test4", OrderSide.SELL, new BigDecimal("73960"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("73960")), eq(BigDecimal.ONE), any());
		verify(mockedSimulatedTradesExchangeClient, times(2)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
	}

	@Test
	public void noSellAfterZeroticks() {
		TaggedTrade trade;
		trade = new TaggedTrade("test1", OrderSide.BUY, new BigDecimal("74010"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test2", OrderSide.BUY, new BigDecimal("74020"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test3", OrderSide.BUY, new BigDecimal("74030"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("74030")), eq(BigDecimal.ONE), any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test4", OrderSide.BUY, new BigDecimal("74030"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("74030")), eq(BigDecimal.ONE), any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test5", OrderSide.BUY, new BigDecimal("74030"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("74030")), eq(BigDecimal.ONE), any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
	}
	
	@Test
	public void sellAfterZeroticks() {
		TaggedTrade trade;
		trade = new TaggedTrade("test1", OrderSide.BUY, new BigDecimal("74010"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test2", OrderSide.BUY, new BigDecimal("74020"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test3", OrderSide.BUY, new BigDecimal("74020"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test4", OrderSide.BUY, new BigDecimal("74020"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test5", OrderSide.BUY, new BigDecimal("74030"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("74030")), eq(BigDecimal.ONE), any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
	}	

	@Test
	public void noBuyAfterZeroticks() {
		TaggedTrade trade;
		trade = new TaggedTrade("test1", OrderSide.SELL, new BigDecimal("73990"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test2", OrderSide.SELL, new BigDecimal("73980"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("73980")), eq(BigDecimal.ONE), any());

		trade = new TaggedTrade("test3", OrderSide.SELL, new BigDecimal("73980"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("73980")), eq(BigDecimal.ONE), any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test4", OrderSide.SELL, new BigDecimal("73980"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("73980")), eq(BigDecimal.ONE), any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
	}
	
	@Test
	public void buyAfterZeroticks() {
		TaggedTrade trade;
		trade = new TaggedTrade("test1", OrderSide.SELL, new BigDecimal("73990"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test2", OrderSide.SELL, new BigDecimal("73990"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test3", OrderSide.SELL, new BigDecimal("73990"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test4", OrderSide.SELL, new BigDecimal("73980"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("73980")), eq(BigDecimal.ONE), any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
	}
	
	
	@Test
	public void noTradeMixingTicks() {
		TaggedTrade trade;
		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74010"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74020"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.SELL, new BigDecimal("74015"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74018"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74023"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74023"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.SELL, new BigDecimal("74023"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.SELL, new BigDecimal("74023"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.SELL, new BigDecimal("74022"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.SELL, new BigDecimal("74022"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74025"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74029"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.SELL, new BigDecimal("74028"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74032"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74035"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
	}
	

	@Test
	public void tradeMixingTicks() {
		
		TaggedTrade trade;
		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74010"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74020"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.SELL, new BigDecimal("74015"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74018"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74023"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74023"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateSell(any(), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74024"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("74024")), eq(BigDecimal.ONE), any());
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
		
		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74026"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.SELL, new BigDecimal("74025"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.BUY, new BigDecimal("74026"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.SELL, new BigDecimal("74024"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(0)).simulateBuy(any(), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());

		trade = new TaggedTrade("test", OrderSide.SELL, new BigDecimal("74023"), new BigDecimal("0.01"));
		contrarianStrategy.onNewTrade(trade);
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), eq(new BigDecimal("74023")), eq(BigDecimal.ONE), any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateBuy(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
		verify(mockedSimulatedTradesExchangeClient, times(1)).simulateSell(eq(contrarianStrategy.getStrategyId()), any(), any(), any() ,any());
	}
}
