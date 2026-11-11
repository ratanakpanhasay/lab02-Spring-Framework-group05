package edu.itc.shop.web.dto;

import edu.itc.shop.domain.enums.OrderStatus;
import java.time.Instant;

public interface OrderSummary {
    Long getId();
    OrderStatus getStatus();
    Instant getPlacedAt();
    CustomerName getCustomer();

    interface CustomerName {
        String getName();
    }
}
