package net.rubenmartinez.stpc.test.util;

import net.rubenmartinez.stpc.exchange.bitso.configuration.Configuration;

public class TestConfiguration extends Configuration {
	
	private static final String TEST_CONFIGURATION_PROPERTY_BOOKNAME = "test.bookName";
	
    public static final String getTestBookName() {
        return getProperties().getProperty(TEST_CONFIGURATION_PROPERTY_BOOKNAME);
    }
}
