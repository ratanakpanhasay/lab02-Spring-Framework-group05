
package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import edu.itc.shop.domain.enums.Category;
import jakarta.persistence.EntityManager;

@DataJpaTest
class EntityIdentityTest {

    @Autowired
    private EntityManager em;

    @Test
    void productShouldRemainFindableInHashSetAfterPersistence() {
        Product product = new Product(
            "Laptop",
            new Money(new BigDecimal("1000.00"), "USD"),
            Category.ELECTRONICS
        );

        Set<Product> products = new HashSet<>();
        products.add(product);

        // Persist the product and assign its database ID
        em.persist(product);
        em.flush();

        // The hash code must remain stable after persistence
        assertTrue(products.contains(product));

        Long productId = product.getId();

        // Detach all entities and reload the product
        em.clear();

        Product reloadedProduct = em.find(Product.class, productId);

        assertFalse(product == reloadedProduct);
        assertEquals(product, reloadedProduct);
        assertNotSame(product, reloadedProduct);
    }
}

