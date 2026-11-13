
package edu.itc.shop.service;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Address;
import edu.itc.shop.domain.Customer;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.enums.OrderStatus;
import edu.itc.shop.repository.CustomerRepository;
import edu.itc.shop.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void dirtyCheckingAutomaticallyUpdatesOrderStatus() {

        // Arrange: create a customer
        String email = UUID.randomUUID() + "@example.com";

        Customer customer = customers.save(
                new Customer(
                        "Test Customer",
                        email,
                        new Address(
                                "123 Main Street",
                                "Phnom Penh",
                                "Cambodia",
                                "12000"
                        )
                )
        );

        // Create an order with default PENDING status
        Order order = orders.save(new Order(customer));

        Long orderId = order.getId();

        // Flush setup operations before measuring updates
        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory =
                entityManagerFactory.unwrap(SessionFactory.class);

        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        // Act: change status through markPaid()
        orderService.markPaid(orderId);

        // Force Hibernate to execute the UPDATE
        entityManager.flush();

        // Verify exactly one entity UPDATE
        assertThat(statistics.getEntityUpdateCount())
                .isEqualTo(1);

        // Clear persistence context to force database retrieval
        entityManager.clear();

        // Assert: verify persisted status
        Order updatedOrder = orders.findById(orderId)
                .orElseThrow();

        assertThat(updatedOrder.getStatus())
                .isEqualTo(OrderStatus.PAID);
    }
}
