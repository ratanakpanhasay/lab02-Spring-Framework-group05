package edu.itc.shop.web.dto;

import edu.itc.shop.domain.enums.OrderStatus;
import java.time.Instant;
import java.util.List;

public record OrderDetail(Long id, OrderStatus status, Instant placedAt, String customerName,
                          String customerEmail, List<OrderLineView> lines, MoneyView total) {}
