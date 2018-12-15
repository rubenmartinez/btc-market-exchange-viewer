package net.rubenmartinez.stpc.exchange.domain;

import java.time.ZonedDateTime;

public interface Trade {
    public String getTradeId();

    public ZonedDateTime getCreationDate();
    
    public String getAmount();
    
    public OrderSide getMakerSide();
    
    public String getPrice();
    
}
