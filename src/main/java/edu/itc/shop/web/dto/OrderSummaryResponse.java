package edu.itc.shop.web.dto;

import edu.itc.shop.domain.enums.OrderStatus;
import java.time.Instant;

/** Materializes the projection inside the transaction, including its nested customer. */
public record OrderSummaryResponse(Long id, OrderStatus status, Instant placedAt, Name customer)
        implements OrderSummary {
    @Override public Long getId() { return id; }
    @Override public OrderStatus getStatus() { return status; }
    @Override public Instant getPlacedAt() { return placedAt; }
    @Override public CustomerName getCustomer() { return customer; }

    public record Name(String name) implements OrderSummary.CustomerName {
        @Override public String getName() { return name; }
    }
}
