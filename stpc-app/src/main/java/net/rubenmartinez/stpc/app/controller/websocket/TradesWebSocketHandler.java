package net.rubenmartinez.stpc.app.controller.websocket;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rubenmartinez.stpc.app.exchange.domain.TaggedTrade;
import net.rubenmartinez.stpc.app.exchange.service.listener.TaggedTradeListener;
import net.rubenmartinez.stpc.app.helper.websocket.annotation.WebSocketEndpointPath;

/**
 * This is not a strictly Controller but placed in a subpackage of net.rubenmartinez.stpc.app.controller
 * just to gather all "web entrypoints" together and accesible under the same package
 */
@Component
@WebSocketEndpointPath("/trades")
public class TradesWebSocketHandler extends TextWebSocketHandler implements TaggedTradeListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TradesWebSocketHandler.class);
	
	@Autowired
	private ObjectMapper objectMapper;
	
	private List<WebSocketSession> clientSessions = new CopyOnWriteArrayList<>();
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);
		clientSessions.add(session);
		LOGGER.debug("Connection established: {}; current sessions: [{}]", session, clientSessions.size());
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) {
		LOGGER.debug("Unexpected message from session [{}], message: {}", session, message);
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		clientSessions.remove(session);
		LOGGER.debug("Client session [{}] closed with status: [{}]; current sessions: [{}]", session, status, clientSessions.size());
	}
	
	@Override
	public void onNewTrade(TaggedTrade trade) {
		Optional<TextMessage> textMessage = tradeToTextMessage(trade);
		if (textMessage.isPresent()) {
			clientSessions.forEach(session -> safeSendMessage(session, textMessage.get()));
		}
	}

	private Optional<TextMessage> tradeToTextMessage(TaggedTrade trade) {
		try {
			String tradeJsonText = objectMapper.writeValueAsString(trade);
			return Optional.ofNullable(new TextMessage(tradeJsonText));
		} catch (JsonProcessingException e) {
			LOGGER.error("Exception while parsing trade object as a JSON String: {}", trade);
			return Optional.empty();
		}
	}
	
	private static void safeSendMessage(WebSocketSession session, TextMessage textMessage) {
		try {
			session.sendMessage(textMessage);
		} catch (IOException e) {
			LOGGER.error("(Swallowing) Exception while sending text message to session [{}]. Message: {}", session, textMessage);
		}
	}
}
