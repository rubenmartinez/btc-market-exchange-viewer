package net.rubenmartinez.stpc.app.strategy.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.rubenmartinez.stpc.app.Main;
import net.rubenmartinez.stpc.app.strategy.ReconfigurableReevaluatingStrategy;
import net.rubenmartinez.stpc.app.strategy.Strategy;
import net.rubenmartinez.stpc.app.strategy.service.exception.ConfigurationParseException;
import net.rubenmartinez.stpc.app.strategy.service.exception.StrategyNotFoundException;
import net.rubenmartinez.stpc.app.strategy.service.exception.StrategyNotReconfigurableException;

@Service
public class StrategyService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	
    private static ObjectMapper jsonObjectMapper = new ObjectMapper(); // Static as ObjectMapper is ensured to be thread safe
	
	@Autowired
	private List<Strategy> configuredStrategies;
	
	private Map<String, Strategy> configuredStrategiesById;
	
	@PostConstruct
	public void init() {
		configuredStrategiesById = configuredStrategies.stream().collect(Collectors.toMap(Strategy::getStrategyId, Function.identity()));
		
		LOGGER.debug("initiated");
	}
	
	public List<Strategy> findAll() {
		return configuredStrategies;
	}
	
	public Strategy findById(String id) {
		Strategy strategy = configuredStrategiesById.get(id);
		if (strategy == null) {
			throw new StrategyNotFoundException("Strategy not found with id: " + id);
		}
		return strategy;
	}

	public <T> void reconfigureStrategy(String id, String jsonTree) {
		ReconfigurableReevaluatingStrategy<T> reconfigurableStrategy = getReconfigurableStrategyById(id);

		try {
			T config = jsonObjectMapper.readValue(jsonTree, reconfigurableStrategy.getConfigType());
			reconfigurableStrategy.reconfigure(config);
		} catch (IOException e) {
			throw new ConfigurationParseException("Couldn't parse new configuration for strategy id ["+id+"]. Configuration: " + jsonTree, e);
		}
	}
	
	private <T> ReconfigurableReevaluatingStrategy<T> getReconfigurableStrategyById(String id) {
		
		Strategy strategy = findById(id);
		
		if (!(strategy instanceof ReconfigurableReevaluatingStrategy)) {
			throw new StrategyNotReconfigurableException("Strategy " + strategy.getStrategyId() + " is not reconfigurable");
		}
		@SuppressWarnings("unchecked")
		ReconfigurableReevaluatingStrategy<T> reconfigurableStrategy = (ReconfigurableReevaluatingStrategy<T>) strategy;
		return reconfigurableStrategy;
	}

}
