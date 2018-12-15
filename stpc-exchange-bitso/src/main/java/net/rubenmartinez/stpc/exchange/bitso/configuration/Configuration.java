package net.rubenmartinez.stpc.exchange.bitso.configuration;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubenmartinez.stpc.exchange.bitso.BitsoExchangeException;
import net.rubenmartinez.stpc.exchange.bitso.util.Resources;

/**
 *  
 * Singleton
 * 
 */
public class Configuration {

	private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
    
    private static final String PROPERTIES_FILE = "/bitso.properties";
    private static final String PROPERTY_REST_ENDPOINT = "rest.endpoint";
    private static final String PROPERTY_WEBSOCKET_ENDPOINT = "websocket.endpoint";
    private static final String PROPERTY_WEBSOCKET_SUBSCRIBE_MESSAGE_DIFFORDERS = "websocket.subscribe.diff-orders";
    private static final String PROPERTY_WEBSOCKET_MISSING_SEQUENCE_WHILE_RESETTING_MAX_RETRIES = "websocket.missing-sequence-while-resetting-max-retries";
    private static final String PROPERTY_TRADE_HOLDER_POLL_SECONDS = "trade-holder.poll.seconds";
    private static final String PROPERTY_TRADE_HOLDER_POLL_TRADES = "trade-holder.poll.trades";
    private static final String PROPERTY_TRADE_HOLDER_BUFFER_MAX_TRADES = "trade-holder.buffer.max-trades";
    private static final String PROPERTY_TRADE_HOLDER_POLL_ITERATIONS_MILLIS_WAIT_FINDING_LAST_TRADE = "trade-holder.poll.iterations-millis-wait-finding-last-trade";
    private static final String PROPERTY_ORDERBOOK_READY_TIMEOUT_SECONDS = "orderbook.ready-timeout-seconds";
    private static final String PROPERTY_ORDERBOOK_RESET_RETRY_WAIT_MILLIS = "orderbook.reset-retry-millis-wait";
    
    
    private static Configuration theInstance = new Configuration();
    
    private Properties properties;
    
    protected Configuration() {
    	LOGGER.debug("Creating configuration");
        this.properties = new Properties();
        InputStream propertiesIS = Resources.getResourceAsStream(PROPERTIES_FILE);
        try {
            properties.load(propertiesIS);
            LOGGER.trace("Properties read: {}", properties);
        } catch (Exception e) {
            throw new BitsoExchangeException("Error while reading properties file from classpath: "+PROPERTIES_FILE, e);
        }
    }
    
    public static final int getOrderBookResetRetryWaitMillis() {
    	return getIntProperty(PROPERTY_ORDERBOOK_RESET_RETRY_WAIT_MILLIS);
    }
    
    public static final String getRestEndpointUri() {
        return getStringProperty(PROPERTY_REST_ENDPOINT);
    }
    
    public static final String getWebsocketEndpointUri() {
        return getStringProperty(PROPERTY_WEBSOCKET_ENDPOINT);
    }
    
    public static final String getWebsocketSubscribeMessageDiffOrders() {
    	return getStringProperty(PROPERTY_WEBSOCKET_SUBSCRIBE_MESSAGE_DIFFORDERS);
    }
    
    public static final int getWebsocketMissingSequenceWhileResettingMaxRetries() {
    	return getIntProperty(PROPERTY_WEBSOCKET_MISSING_SEQUENCE_WHILE_RESETTING_MAX_RETRIES);
    }
    
    public static final int getTradeHolderBufferMaxTrades() {
    	return getIntProperty(PROPERTY_TRADE_HOLDER_BUFFER_MAX_TRADES);
    }
    
    public static final int getTradeHolderPollSeconds() {
    	return getIntProperty(PROPERTY_TRADE_HOLDER_POLL_SECONDS);
    }
    
    public static final int getTradeHolderPollTrades() {
    	return getIntProperty(PROPERTY_TRADE_HOLDER_POLL_TRADES);
    }

    public static final int getTradeHolderIterationsMillisWaitFindingLastTrade() {
    	return getIntProperty(PROPERTY_TRADE_HOLDER_POLL_ITERATIONS_MILLIS_WAIT_FINDING_LAST_TRADE);
    }
    
	public static long getOrderBookReadyTimeoutSeconds() {
		return getIntProperty(PROPERTY_ORDERBOOK_READY_TIMEOUT_SECONDS);
	}
    

    private static final int getIntProperty(String name) {
    	try {
        	return Integer.parseInt(getStringProperty(name));
    	} catch (NumberFormatException e) {
    		throw new IllegalArgumentException("Property [" + name + "] must be a number", e);
    	}
    }

    private static final String getStringProperty(String name) {
    	return getProperties().getProperty(name);
    }

    public static Properties getProperties() {
        return theInstance.properties;
    }
    
}
