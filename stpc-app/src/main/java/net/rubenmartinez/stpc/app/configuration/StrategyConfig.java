package net.rubenmartinez.stpc.app.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.rubenmartinez.stpc.app.exchange.decorator.SimulatedTradesExchangeClient;
import net.rubenmartinez.stpc.app.strategy.Strategy;
import net.rubenmartinez.stpc.app.strategy.implementations.contrarian.ContrarianStrategy;
import net.rubenmartinez.stpc.app.strategy.implementations.contrarian.ContrarianStrategyConfig;

@Configuration
public class StrategyConfig {

	@Bean("contrarian1")
	public Strategy getContrarianStrategy(@Autowired ContrarianStrategyConfig defaultConfig, @Autowired SimulatedTradesExchangeClient exchangeClient) {
		Strategy contrarianStrategy = new ContrarianStrategy("contrarian1", defaultConfig, exchangeClient);
		contrarianStrategy.activate();
		return contrarianStrategy;
	}

	@Bean("just-a-second-one-for-demonstration-it-could-have-other-type-also")
	public Strategy getSecondContrarianStrategy(@Autowired ContrarianStrategyConfig defaultConfig, @Autowired SimulatedTradesExchangeClient exchangeClient) {
		return new ContrarianStrategy("just-a-second-one-for-demonstration-it-could-have-other-type-also", defaultConfig, exchangeClient);
	}
	
	@Bean
	public ContrarianStrategyConfig getDefaultContrarianStrategyConfig(
			@Value("${strategy.contrarian.upticksToSell}") int downticks,
			@Value("${strategy.contrarian.downticksToBuy}") int upticks) {
		ContrarianStrategyConfig config = new ContrarianStrategyConfig();
		config.setConsecutiveUpticksToSell(upticks);
		config.setConsecutiveDownticksToBuy(downticks);

		return config;
	}
}
