package edu.itc.shop.web.dto;

public record OrderLineView(Long id, Long productId, String sku, String productName,
                            int quantity, MoneyView unitPrice) {}
