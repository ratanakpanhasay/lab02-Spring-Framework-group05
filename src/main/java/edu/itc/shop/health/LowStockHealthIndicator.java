package edu.itc.shop.health;

import java.util.List;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import edu.itc.shop.config.ShopProperties;
import edu.itc.shop.domain.Product;
import edu.itc.shop.repository.ProductRepository;

@Component
public class LowStockHealthIndicator implements HealthIndicator {

    private final ProductRepository products;
    private final ShopProperties properties;

    public LowStockHealthIndicator(
            ProductRepository products,
            ShopProperties properties) {
        this.products = products;
        this.properties = properties;
    }

    @Override
    public Health health() {

        List<String> lowStockProducts = products.findAll()
                .stream()
                .filter(product ->
                        product.stock() < properties.lowStockThreshold())
                .map(Product::name)
                .toList();

        if (!lowStockProducts.isEmpty()) {
            return Health.down()
                    .withDetail("lowStockProducts", lowStockProducts)
                    .build();
        }

        return Health.up()
                .withDetail("message", "All products have sufficient stock")
                .build();
    }
}
