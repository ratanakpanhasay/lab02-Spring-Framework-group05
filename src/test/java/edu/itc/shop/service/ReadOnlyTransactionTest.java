
package edu.itc.shop.service;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import edu.itc.shop.domain.Address;
import edu.itc.shop.domain.Customer;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.enums.OrderStatus;
import edu.itc.shop.repository.CustomerRepository;
import edu.itc.shop.repository.OrderRepository;
import edu.itc.shop.web.dto.OrderDto;
import jakarta.persistence.EntityManager;

@SpringBootTest
class ReadOnlyTransactionTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate tx;

    @Test
    void readOnlyTransactionDoesNotFlushChanges() {

        // Arrange: create data in a real transaction
        Long orderId = tx.execute(status -> {

            String email = UUID.randomUUID() + "@example.com";

            Customer customer = customers.save(
                    new Customer(
                            "Read Only Customer",
                            email,
                            new Address(
                                    "123 Main Street",
                                    "Phnom Penh",
                                    "Cambodia",
                                    "12000"
                            )
                    )
            );

            Order order = orders.save(new Order(customer));

            entityManager.flush();

            return order.getId();
        });

        // Act: call the separate read-only service transaction
        OrderDto result = orderService.view(orderId);

        // The in-memory entity was modified
        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);

        // Assert: reload from database in a new transaction
        OrderStatus databaseStatus = tx.execute(status -> {
            entityManager.clear();

            return orders.findById(orderId)
                    .orElseThrow()
                    .getStatus();
        });

        assertThat(databaseStatus).isEqualTo(OrderStatus.PENDING);
    }
}
