package net.rubenmartinez.stpc.exchange.bitso.api.rest.deserialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import net.rubenmartinez.stpc.exchange.domain.OrderSide;

/**
 * 
 */
public class OrderSideDeserializer extends StdScalarDeserializer<OrderSide> {

	private static final long serialVersionUID = 1L;

	protected OrderSideDeserializer() {
		super(OrderSide.class);
	}

	@Override
	public OrderSide deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
		String orderSide = jp.getValueAsString();
		return OrderSide.valueOf(orderSide.toUpperCase());
	}
}
