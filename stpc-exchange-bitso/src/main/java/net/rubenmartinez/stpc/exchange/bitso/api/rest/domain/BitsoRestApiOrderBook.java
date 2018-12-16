// Generated by delombok at Sat Dec 15 11:30:27 CET 2018
package net.rubenmartinez.stpc.exchange.bitso.api.rest.domain;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import net.rubenmartinez.stpc.exchange.bitso.api.rest.deserialize.BitsoZonedDateTimeDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitsoRestApiOrderBook {
	private boolean success;
	private Payload payload;


	public static class Payload {
		private List<Item> asks;
		private List<Item> bids;
		@JsonAlias("updated_at")
		@JsonDeserialize(using = BitsoZonedDateTimeDeserializer.class)
		private ZonedDateTime updated;
		private long sequence;


		public static class Item {
			@JsonProperty("oid")
			private String orderId;
			private String book;
			private String price;
			private String amount;

			public String getOrderId() {
				return this.orderId;
			}

			public String getBook() {
				return this.book;
			}

			public String getPrice() {
				return this.price;
			}

			public String getAmount() {
				return this.amount;
			}

			public void setOrderId(final String orderId) {
				this.orderId = orderId;
			}

			public void setBook(final String book) {
				this.book = book;
			}

			public void setPrice(final String price) {
				this.price = price;
			}

			public void setAmount(final String amount) {
				this.amount = amount;
			}

			@Override
			public String toString() {
				return "BitsoRestApiOrderBook.Payload.Item(orderId=" + this.getOrderId() + ", book=" + this.getBook() + ", price=" + this.getPrice() + ", amount=" + this.getAmount() + ")";
			}
		}

		public List<Item> getAsks() {
			return this.asks;
		}

		public List<Item> getBids() {
			return this.bids;
		}

		public ZonedDateTime getUpdated() {
			return this.updated;
		}

		public long getSequence() {
			return this.sequence;
		}

		public void setAsks(final List<Item> asks) {
			this.asks = asks;
		}

		public void setBids(final List<Item> bids) {
			this.bids = bids;
		}

		public void setUpdated(final ZonedDateTime updated) {
			this.updated = updated;
		}

		public void setSequence(final long sequence) {
			this.sequence = sequence;
		}

		@Override
		public String toString() {
			return "BitsoRestApiOrderBook.Payload(asks=" + this.getAsks() + ", bids=" + this.getBids() + ", updated=" + this.getUpdated() + ", sequence=" + this.getSequence() + ")";
		}
	}

	public boolean isSuccess() {
		return this.success;
	}

	public Payload getPayload() {
		return this.payload;
	}

	public void setSuccess(final boolean success) {
		this.success = success;
	}

	public void setPayload(final Payload payload) {
		this.payload = payload;
	}

	@Override
	public String toString() {
		return "BitsoRestApiOrderBook(success=" + this.isSuccess() + ", payload=" + this.getPayload() + ")";
	}
}