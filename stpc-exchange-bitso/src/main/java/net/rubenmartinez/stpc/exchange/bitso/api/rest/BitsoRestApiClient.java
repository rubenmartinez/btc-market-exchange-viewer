package net.rubenmartinez.stpc.exchange.bitso.api.rest;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.domain.BitsoRestApiOrderBook;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.domain.BitsoRestApiTrades;

/**
 *
 * User-Agent is the only string got from the official API. Needed to avoid error: "The owner of this website (api.bitso.com) has banned your access based on your browser's signature"
 * 
 */
@Headers("User-Agent: Android")
public interface BitsoRestApiClient {
    @RequestLine("GET /v3/trades/?book={book}&marker={marker}&sort={sort}&limit={limit}")
    public BitsoRestApiTrades getTrades(@Param("book") String book, @Param("marker") String markerTradeId, @Param("sort") TradesSort directionFromMarker, @Param("limit") int limit);
    
    @RequestLine("GET /v3/trades/?book={book}&limit={limit}")
    public BitsoRestApiTrades getNewestTrades(@Param("book") String book, @Param("limit") int limit);

	@RequestLine("GET /v3/order_book/?book={book}&aggregate={aggregate}")
	public BitsoRestApiOrderBook getOrderBook(@Param("book") String book, @Param("aggregate") boolean aggregate);

	public enum TradesSort {
		ASC, DESC;
		
		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
	}

}

