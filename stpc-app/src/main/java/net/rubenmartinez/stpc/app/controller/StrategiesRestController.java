package net.rubenmartinez.stpc.app.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.rubenmartinez.stpc.app.strategy.Strategy;
import net.rubenmartinez.stpc.app.strategy.service.StrategyService;

/**
 * REST Controller to manage strategies. 
 * 
 * This REST Controller would normally also create more Strategy <em>instances</em> of a given type on http POST method, etc...
 */
@RestController
@RequestMapping("/api/v1/strategies")
public class StrategiesRestController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategiesRestController.class);

	
	@Autowired
	private StrategyService strategyService;
	

	@GetMapping
	public List<Strategy> strategies() {
		return strategyService.findAll();
	}

	@GetMapping("/{strategyId}")
	private Strategy getStrategyById(@PathVariable String strategyId) {
		LOGGER.debug("getStrategyById: {}", strategyId);
		return strategyService.findById(strategyId);
	}
	
	@PutMapping("/{strategyId}/reconfigure")
	private void reconfigure(@PathVariable String strategyId, @RequestBody String body) {
		strategyService.reconfigureStrategy(strategyId, body);
	}
	
}
