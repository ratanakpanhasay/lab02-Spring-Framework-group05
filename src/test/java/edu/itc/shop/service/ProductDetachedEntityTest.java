
package edu.itc.shop.service;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.enums.Category;
import edu.itc.shop.repository.ProductRepository;

@SpringBootTest
class ProductDetachedEntityTest {

    @Autowired
    private ProductRepository products;

    @Test
    void detachedEntityRequiresSaveToPersistChanges() {

        // Arrange: create and save a product
        String sku = "TEST-" + UUID.randomUUID();

        Product original = new Product(
                sku,
                "Test Product",
                new Money(new BigDecimal("25.00"), "USD"),
                Category.HARDWARE,
                10
        );

        Product saved = products.save(original);
        Long productId = saved.getId();

        // Step 1: Load product outside a surrounding transaction.
        // The returned entity is detached.
        Product p = products.findById(productId)
                .orElseThrow();

        // Step 2: Modify the detached entity
        p.changeStock(99);

        // Step 3: Reload in a new repository call.
        // The database should still contain the original stock.
        Product unchanged = products.findById(productId)
                .orElseThrow();

        assertThat(unchanged.getStock()).isEqualTo(10);

        // Step 4: Save the detached entity (merge)
        products.save(p);

        // Step 5: Reload and verify the change was persisted
        Product updated = products.findById(productId)
                .orElseThrow();

        assertThat(updated.getStock()).isEqualTo(99);
    }
}
