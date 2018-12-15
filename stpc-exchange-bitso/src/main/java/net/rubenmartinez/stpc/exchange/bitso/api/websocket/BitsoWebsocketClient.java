package net.rubenmartinez.stpc.exchange.bitso.api.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rubenmartinez.stpc.exchange.bitso.BitsoExchangeException;
import net.rubenmartinez.stpc.exchange.bitso.api.websocket.domain.DiffOrdersWebsocketMessage;
import net.rubenmartinez.stpc.exchange.bitso.configuration.Configuration;

/**
 * Set listeners before
 * 
 *
 */
@ClientEndpoint
public class BitsoWebsocketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitsoWebsocketClient.class);

    private static final String MESSAGE_PROPERTY_TYPE = "type";
    private static final String MESSAGE_PROPERTY_ACTION = "action";
    private static final String MESSAGE_PROPERTY_RESPONSE = "response";
    private static final String MESSAGE_PROPERTY_RESPONSE_OK = "ok";

    private static final String MESSAGE_TYPE_DIFF_ORDERS = "diff-orders";
    private static final String MESSAGE_ACTION_SUBSCRIBE = "subscribe";
    
    private static final long CONNECTION_RETRY_WAIT_MILLIS = 2000;

    private static ObjectMapper jsonObjectMapper = new ObjectMapper(); // Static as ObjectMapper is ensured to be thread safe

    private WebSocketContainer webSocketContainer;
    private Session webSocketSession;
    private DiffOrdersListener diffOrdersListener;

    public BitsoWebsocketClient(DiffOrdersListener diffOrdersListener) {
    	if (diffOrdersListener == null) {
    		throw new IllegalArgumentException("DiffOrdersListener is a mandatory argument");
    	}
    	
    	this.diffOrdersListener = diffOrdersListener;

		try {
			webSocketContainer = ContainerProvider.getWebSocketContainer();
			LOGGER.debug("Created JSR-356 WebSocketContainer: {}", webSocketContainer);
		} catch (Exception e) {
			throw new BitsoExchangeException("Error creating WebSocketContainer from classpath", e);
		}

        LOGGER.debug("Constructed");
    }
    
	public void start() {
		connect();
	}
	
	public void stop() {
		if (webSocketSession != null) {
			try {
				webSocketSession.close();
			} catch (IOException e) {
				LOGGER.error("Error while closing webSocketSession", e);
			}
		}
	}
	
	private void reconnect() {
		LOGGER.info("Reconnecting");
		stop();
		start();
	}
	
	private void connect() {
		boolean connected = false;
		do {
			LOGGER.debug("Connecting");
			try {
				BitsoWebsocketClient websocketClientEndpoint = new BitsoWebsocketClient((DiffOrdersListener) this.diffOrdersListener);
				webSocketSession = webSocketContainer.connectToServer(websocketClientEndpoint, new URI(Configuration.getWebsocketEndpointUri()));
				connected = true;
			} catch (Exception e) {
				LOGGER.error("Error creating websocket to [" + Configuration.getWebsocketEndpointUri() + "]", e);
				sleepMillis(CONNECTION_RETRY_WAIT_MILLIS);
			}
		} while (!connected);
	}

	
    
    @OnOpen
    public void onOpen(Session session) {
        LOGGER.info("WebSocket opened: {}", session.getId());
        
        String diffOrdersSubscribeMessage = Configuration.getWebsocketSubscribeMessageDiffOrders();
        try {
			session.getBasicRemote().sendText(diffOrdersSubscribeMessage);
		} catch (Exception e) {
			throw new BitsoExchangeWebSocketException("Error sending subscribe message: " + diffOrdersSubscribeMessage, e);
		}
    }

    @OnMessage
    public void onMessage(String jsonMessage) {
        LOGGER.debug("New websocket message received: {}", jsonMessage);

        try {
            JsonNode rootNode = jsonObjectMapper.readTree(jsonMessage);
            
            String action = nodeAsText(rootNode, MESSAGE_PROPERTY_ACTION).orElse("");
            
            if (action.equals(MESSAGE_ACTION_SUBSCRIBE)) {
            	LOGGER.info("Message received is a subscribe response message: {}", jsonMessage);
                handleSubscribeResponseMessage(jsonMessage, rootNode);
            }
            else {
            	// Validating that client is subscribed before processing any other type has been left out on purpose (we just need diff orders, we don't really care if subscribe configuration was lost)
                String type = nodeAsText(rootNode, MESSAGE_PROPERTY_TYPE).orElse("");
                
                if (MESSAGE_TYPE_DIFF_ORDERS.equals(type)) {
                	LOGGER.debug("Message received is a diff-orders message: {}", jsonMessage);
                    handleDiffOrdersMessage(jsonMessage, rootNode);
                }
                else {
                	LOGGER.debug("Ignoring message with type: [{}]: {}", type, jsonMessage);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Message caused exception. Fail-safe procedure ignoring: {}", jsonMessage, e);
        }
    }

    private void handleDiffOrdersMessage(String jsonMessage, JsonNode rootNode) throws JsonProcessingException {
        LOGGER.debug("Received diffOrders message: {} ", jsonMessage);
        
        DiffOrdersWebsocketMessage diffOrdersMessage = jsonObjectMapper.treeToValue(rootNode, DiffOrdersWebsocketMessage.class);
        diffOrdersListener.onDiffOrder(diffOrdersMessage);        
    }

    private void handleSubscribeResponseMessage(String jsonMessage, JsonNode rootNode) {
        String response = nodeAsText(rootNode, MESSAGE_PROPERTY_RESPONSE).orElse("");
        if (!response.equals(MESSAGE_PROPERTY_RESPONSE_OK)) {
            LOGGER.warn("non-ok response in subscribe message: {}", jsonMessage);
        }

    }
    
    private static final Optional<String> nodeAsText(JsonNode node, String propertyName) {
        JsonNode value = node.get(propertyName);
        if (value == null) {
            return Optional.empty();
        }
        else {
            return Optional.of(value.asText()); 
        }
    }

    @OnClose
    public void onClose(CloseReason reason) {
        LOGGER.info("WebSocket closed. Reason code: [{}], Reason phrase: {}", reason.getCloseCode(), reason.getReasonPhrase());
        if (reason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE) {
            LOGGER.warn("WebSocket not closed normally. Trying to reconnect");
            reconnect();
        }
    }
    
	private static void sleepMillis(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			LOGGER.warn("Sleep interrupted");
			Thread.currentThread().interrupt();
		}
	}	    
}
