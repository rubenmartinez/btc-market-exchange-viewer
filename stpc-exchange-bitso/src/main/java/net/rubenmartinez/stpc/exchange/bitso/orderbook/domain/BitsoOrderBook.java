// Generated by delombok at Sat Dec 15 11:30:27 CET 2018
package net.rubenmartinez.stpc.exchange.bitso.orderbook.domain;

import java.util.List;

import net.rubenmartinez.stpc.exchange.domain.Order;
import net.rubenmartinez.stpc.exchange.domain.OrderBook;

public class BitsoOrderBook implements OrderBook {
    private String pair;
    private List<Order> asks;
    private List<Order> bids;

    public String getPair() {
        return this.pair;
    }

    public List<Order> getAsks() {
        return this.asks;
    }

    public List<Order> getBids() {
        return this.bids;
    }

    public void setPair(final String pair) {
        this.pair = pair;
    }

    public void setAsks(final List<Order> asks) {
        this.asks = asks;
    }

    public void setBids(final List<Order> bids) {
        this.bids = bids;
    }

    @Override
    public String toString() {
        return "BitsoOrderBook(pair=" + this.getPair() + ", asks=" + this.getAsks() + ", bids=" + this.getBids() + ")";
    }
}