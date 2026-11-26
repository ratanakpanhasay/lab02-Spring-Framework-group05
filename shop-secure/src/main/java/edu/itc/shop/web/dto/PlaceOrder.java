package edu.itc.shop.web.dto;

import jakarta.validation.constraints.Positive;

public record PlaceOrder(
        @Positive long productId,
        @Positive int quantity
) {}
