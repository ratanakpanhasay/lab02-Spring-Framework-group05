

package edu.itc.shop.domain;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import edu.itc.shop.domain.enums.Category;
import jakarta.persistence.EntityManager;

@DataJpaTest
class OrderCascadeTest {

    @Autowired
    private EntityManager em;

    @Test
    void orderShouldCascadePersistAndRemoveLines() {

        // 1. Create and persist customer and products
        Customer customer = new Customer(
                "John",
                "john@example.com",
                null
        );

        Product laptop = new Product(
                "Laptop",
                new Money(new BigDecimal("1000.00"), "USD"),
                Category.ELECTRONICS
        );

        Product phone = new Product(
                "Phone",
                new Money(new BigDecimal("500.00"), "USD"),
                Category.ELECTRONICS
        );

        em.persist(customer);
        em.persist(laptop);
        em.persist(phone);

        // 2. Create order with two lines
        Order order = new Order(customer);
        order.addLine(laptop, 2);
        order.addLine(phone, 1);

        em.persist(order);
        em.flush();

        // 3. Verify that both lines were persisted
        assertEquals(
                2L,
                em.createQuery(
                        "select count(l) from OrderLine l",
                        Long.class
                ).getSingleResult()
        );

        // 4. Clear persistence context and reload order
        Long orderId = order.getId();

        em.clear();

        Order savedOrder = em.find(Order.class, orderId);

        assertNotNull(savedOrder);
        assertEquals(2, savedOrder.getLines().size());
        assertEquals(
                new BigDecimal("2500.00"),
                savedOrder.total().amount()
        );

        // 5. Remove one line and verify orphan removal
        OrderLine lineToRemove = savedOrder.getLines().get(0);

        assertSame(savedOrder, lineToRemove.getOrder());

        savedOrder.removeLine(lineToRemove);
        em.flush();

        assertEquals(
                1L,
                em.createQuery(
                        "select count(l) from OrderLine l",
                        Long.class
                ).getSingleResult()
        );

        // 6. Remove the whole order and verify cascade REMOVE
        em.remove(savedOrder);
        em.flush();

        assertEquals(
                0L,
                em.createQuery(
                        "select count(l) from OrderLine l",
                        Long.class
                ).getSingleResult()
        );
    }
}
