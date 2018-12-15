package net.rubenmartinez.stpc.exchange.bitso.api.rest.deserialize;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

public class BitsoZonedDateTimeDeserializer extends StdScalarDeserializer<ZonedDateTime> {

	private static final long serialVersionUID = 1L;

	private static final DateTimeFormatter BITSO_ZONED_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][xx][xxx]");
	
	protected BitsoZonedDateTimeDeserializer() {
		super(ZonedDateTime.class);
	}

	@Override
	public ZonedDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
		return ZonedDateTime.parse(jp.getValueAsString(), BITSO_ZONED_DATE_TIME_FORMATTER);
	}
}
