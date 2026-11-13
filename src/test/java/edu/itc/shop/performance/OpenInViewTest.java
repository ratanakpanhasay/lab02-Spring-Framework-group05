
package edu.itc.shop.performance;

import java.math.BigDecimal;

import edu.itc.shop.domain.Customer;
import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.enums.Category;

import jakarta.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.persistence.EntityManagerFactory;

@SpringBootTest
class OpenInViewTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void demonstrateLazyLoadingOutsidePersistenceContext() {

        TransactionTemplate transaction =
            new TransactionTemplate(transactionManager);

        // Create data and retrieve an Order inside a transaction
        Order detachedOrder = transaction.execute(status -> {

            Customer customer = new Customer(
                "OSIV Customer",
                "osiv@example.com",
                null
            );

            Product product = new Product(
                "OSIV Test Product",
                new Money(new BigDecimal("19.99"), "USD"),
                Category.ELECTRONICS
            );

            entityManager.persist(customer);
            entityManager.persist(product);

            Order order = new Order(customer);
            order.addLine(product, 1);
            entityManager.persist(order);

            entityManager.flush();
            Long orderId = order.getId();

            // Remove managed entities from memory
            entityManager.clear();

            // Reload Order without initializing its lines
            Order loaded = entityManager.find(Order.class, orderId);

            assertFalse(
                entityManagerFactory.getPersistenceUnitUtil()
                    .isLoaded(loaded, "lines")
            );

            // Prevent test data from remaining in the database
            status.setRollbackOnly();

            return loaded;
        });

        assertNotNull(detachedOrder);

        // Transaction has ended; lazy collection is uninitialized
        assertThrows(
            LazyInitializationException.class,
            () -> detachedOrder.getLines().size()
        );

        System.out.println(
            "OSIV TEST - LazyInitializationException confirmed"
        );
    }
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;
}
