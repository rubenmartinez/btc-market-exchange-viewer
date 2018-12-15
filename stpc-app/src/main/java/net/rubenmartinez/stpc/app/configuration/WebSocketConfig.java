package net.rubenmartinez.stpc.app.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import net.rubenmartinez.stpc.app.controller.websocket.TradesWebSocketHandler;
import net.rubenmartinez.stpc.app.exchange.decorator.SimulatedTradesExchangeClient;
import net.rubenmartinez.stpc.app.helper.websocket.annotation.WebSocketEndpointPath;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketConfig.class);

	@Autowired
	private TradesWebSocketHandler tradesWebSocketHandler;
	
	@Autowired
	SimulatedTradesExchangeClient exchangeClient;
	
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    	exchangeClient.addFlaggedTradeListener(tradesWebSocketHandler);
        registry.addHandler(tradesWebSocketHandler, getTradesWebSocketHandlerPath());
    }
    
    
    private String getTradesWebSocketHandlerPath() {
    	WebSocketEndpointPath webSocketEndpointAnnotation = TradesWebSocketHandler.class.getAnnotation(WebSocketEndpointPath.class);
    	String[] value = webSocketEndpointAnnotation.value();
    	String path = value == null ? "" : value[0];
    	LOGGER.debug("{} marked with path: {}", TradesWebSocketHandler.class.getName(), path);
    	return path;
    }
}
