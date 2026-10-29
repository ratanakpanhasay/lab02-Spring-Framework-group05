
package edu.itc.shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@ConfigurationProperties(prefix = "shop")
@Validated
public record ShopProperties(
        @NotBlank String currency,

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        double taxRate,

        @PositiveOrZero int lowStockThreshold
) {
}
