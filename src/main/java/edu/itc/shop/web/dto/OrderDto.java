
package edu.itc.shop.web.dto;

import java.time.Instant;

import edu.itc.shop.domain.enums.OrderStatus;

public record OrderDto(
        Long id,
        OrderStatus status,
        Instant placedAt
) {
}
