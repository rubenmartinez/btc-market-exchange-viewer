package net.rubenmartinez.stpc.app.controller;

import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.rubenmartinez.stpc.app.exchange.domain.TaggedTrade;
import net.rubenmartinez.stpc.app.exchange.service.ExchangeService;
import net.rubenmartinez.stpc.exchange.domain.Order;

/**
 * 
 */
@RestController
@RequestMapping("/api/v1/exchange")
public class ExchangeRestController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRestController.class);

	@Resource
	private ExchangeService exchangeService;

	/**
	 * {@see ExchangeService#getAsks(int)}
	 */
	@GetMapping("bestAsks")
	private List<Order> getBestAsks(@RequestParam int size) {
		LOGGER.debug("getBestAsks({})", size);
		return exchangeService.getAsks(size);
	}

	/**
	 * {@see ExchangeService#getBids(int)}
	 */
	@GetMapping("bestBids")
	private List<Order> getBestBids(@RequestParam int size) {
		LOGGER.debug("getBestBids({})", size);
		return exchangeService.getBids(size);
	}

	/**
	 * {@see ExchangeService#getLastTradesOrderedIncludingSimulated(int, String)}
	 */
	@GetMapping("lastTrades")
	private List<TaggedTrade> getLastTradesIncludingSimulated(@RequestParam int size, @RequestParam String strategyId) {
		LOGGER.debug("getLastTrades(size={}, strategyId={})", size, strategyId);
		return exchangeService.getLastTradesOrderedIncludingSimulated(size, strategyId);
	}
	
}
