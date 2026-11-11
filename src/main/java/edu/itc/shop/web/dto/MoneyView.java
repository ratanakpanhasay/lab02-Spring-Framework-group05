package edu.itc.shop.web.dto;

import java.math.BigDecimal;

public record MoneyView(BigDecimal amount, String currency) {}
