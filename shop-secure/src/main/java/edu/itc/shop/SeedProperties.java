package edu.itc.shop;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Seed passwords for the development-time user accounts ({@code admin} and {@code alice}).
 *
 * <p>Bound from {@code shop.seed.*} in {@code application.yml}.
 * Override in production via environment variables:
 * {@code SHOP_SEED_ADMIN_PASSWORD} / {@code SHOP_SEED_CUSTOMER_PASSWORD}.</p>
 *
 * <p><strong>These values are dev-only defaults. Never commit real credentials.</strong></p>
 */
@ConfigurationProperties(prefix = "shop.seed")
public record SeedProperties(

        /** Plain-text password for the seeded {@code admin} account. */
        String adminPassword,

        /** Plain-text password for the seeded {@code alice} account. */
        String customerPassword

) {}
