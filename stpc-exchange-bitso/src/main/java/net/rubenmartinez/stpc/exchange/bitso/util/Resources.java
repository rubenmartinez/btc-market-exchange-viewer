package net.rubenmartinez.stpc.exchange.bitso.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.rubenmartinez.stpc.exchange.bitso.BitsoExchangeException;

public class Resources {
	
	private static final ObjectMapper mapper = new ObjectMapper();

	public static InputStream getResourceAsStream(String resource) {
		try {
			return Resources.class.getResourceAsStream(resource);
		} catch (Exception e) {
			throw new BitsoExchangeException("Error while getting file from classpath: " + resource, e);
		}
	}

	public static String getResourceAsString(String resource) {
		try (Scanner s = new Scanner(getResourceAsStream(resource))) {
			s.useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
		}
	}
	
	public static <T> T getResourceAsParsedJson(String resource, Class<T> valueType) {
		try {
			return mapper.readValue(getResourceAsStream(resource), valueType);
		} catch (IOException e) {
			throw new BitsoExchangeException("Error while parsing resource as Json for type [" + valueType + "]; resource: " + resource, e);
		}
	}
	
	private Resources() {
	}
}
