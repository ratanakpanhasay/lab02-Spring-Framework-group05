
package edu.itc.shop.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Address;
import edu.itc.shop.domain.Customer;
import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.enums.Category;
import edu.itc.shop.repository.CustomerRepository;
import edu.itc.shop.repository.OrderRepository;
import edu.itc.shop.repository.ProductRepository;

@Component
public class TestData {

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;

    public TestData(
            CustomerRepository customers,
            ProductRepository products,
            OrderRepository orders) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
    }

    @Transactional
    public Long newOrder() {

        String email = UUID.randomUUID() + "@example.com";

        Customer customer = customers.save(
                new Customer(
                        "Concurrency Customer",
                        email,
                        new Address(
                                "123 Main Street",
                                "Phnom Penh",
                                "Cambodia",
                                "12000"
                        )
                )
        );

        Product product = products.save(
                new Product(
                        "TEST-" + UUID.randomUUID(),
                        "Concurrency Product",
                        new Money(new BigDecimal("20.00"), "USD"),
                        Category.HARDWARE,
                        10
                )
        );

        Order order = new Order(customer);
        order.addLine(product, 1);

        return orders.save(order).getId();
    }
}
