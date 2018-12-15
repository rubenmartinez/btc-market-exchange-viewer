// Generated by delombok at Sat Dec 15 11:30:27 CET 2018
package net.rubenmartinez.stpc.exchange.bitso.orderbook.domain;

import net.rubenmartinez.stpc.exchange.domain.Order;

public class BitsoOrder implements Order {
    private String id;
    private String price;
    private String amount;

    public BitsoOrder(String id, String price, String amount) {
        // Better not to use Lombok's annotation @AllArgsConstructor with all parameters of equal type, as this would make declaration order relevant
        this.id = id;
        this.price = price;
        this.amount = amount;
    }

    public String getId() {
        return this.id;
    }

    public String getPrice() {
        return this.price;
    }

    public String getAmount() {
        return this.amount;
    }

    @Override
    public String toString() {
        return "BitsoOrder(id=" + this.getId() + ", price=" + this.getPrice() + ", amount=" + this.getAmount() + ")";
    }

    public void setAmount(final String amount) {
        this.amount = amount;
    }
}
