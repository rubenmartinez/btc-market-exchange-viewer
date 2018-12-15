package net.rubenmartinez.stpc.test.util;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import net.rubenmartinez.stpc.exchange.bitso.api.rest.deserialize.BitsoZonedDateTimeDeserializer;
import net.rubenmartinez.stpc.exchange.bitso.api.rest.deserialize.OrderSideDeserializer;
import net.rubenmartinez.stpc.exchange.domain.OrderSide;
import net.rubenmartinez.stpc.exchange.domain.Trade;

public class TestingTrade implements Trade {
	
	private String tradeId;
	@JsonDeserialize(using = BitsoZonedDateTimeDeserializer.class)
	private ZonedDateTime creationDate;
	private String amount;
	@JsonDeserialize(using = OrderSideDeserializer.class)
	private OrderSide makerSide;
	private String price;

	public TestingTrade() {
	}
	
	public TestingTrade(String tradeId, ZonedDateTime creationDate, String amount, OrderSide makerSide, String price) {
		this.tradeId = tradeId;
		this.creationDate = creationDate;
		this.amount = amount;
		this.makerSide = makerSide;
		this.price = price;
	}
	
	public String getTradeId() {
		return tradeId;
	}
	public void setTradeId(String tradeId) {
		this.tradeId = tradeId;
	}
	public ZonedDateTime getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(ZonedDateTime creationDate) {
		this.creationDate = creationDate;
	}
	public String getAmount() {
		return amount;
	}
	public void setAmount(String amount) {
		this.amount = amount;
	}
	public OrderSide getMakerSide() {
		return makerSide;
	}
	public void setMakerSide(OrderSide makerSide) {
		this.makerSide = makerSide;
	}
	public String getPrice() {
		return price;
	}
	public void setPrice(String price) {
		this.price = price;
	}
	
	@Override
	public String toString() {
		return "TestingTrade [tradeId=" + tradeId + ", creationDate=" + creationDate + ", amount=" + amount + ", makerSide=" + makerSide + ", price=" + price + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((amount == null) ? 0 : amount.hashCode());
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((makerSide == null) ? 0 : makerSide.hashCode());
		result = prime * result + ((price == null) ? 0 : price.hashCode());
		result = prime * result + ((tradeId == null) ? 0 : tradeId.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!Trade.class.isInstance(obj))
			return false;
		Trade other = (Trade) obj;
		if (amount == null) {
			if (other.getAmount() != null)
				return false;
		} else if (!amount.equals(other.getAmount()))
			return false;
		if (creationDate == null) {
			if (other.getCreationDate() != null)
				return false;
		} else if (!creationDate.equals(other.getCreationDate()))
			return false;
		if (makerSide != other.getMakerSide())
			return false;
		if (price == null) {
			if (other.getPrice() != null)
				return false;
		} else if (!price.equals(other.getPrice()))
			return false;
		if (tradeId == null) {
			if (other.getTradeId() != null)
				return false;
		} else if (!tradeId.equals(other.getTradeId()))
			return false;
		return true;
	}
}
