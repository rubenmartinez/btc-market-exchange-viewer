package net.rubenmartinez.stpc.app.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.rubenmartinez.stpc.app.exchange.decorator.SimulatedTradesExchangeClient;
import net.rubenmartinez.stpc.exchange.bitso.BitsoExchangeClient;

@Configuration
public class ExchangeConfig {
	@Bean
	public SimulatedTradesExchangeClient getExchangeClient(@Value("${productpair}") String productpair) {
		return new SimulatedTradesExchangeClient(new BitsoExchangeClient(productpair));
	}
}
