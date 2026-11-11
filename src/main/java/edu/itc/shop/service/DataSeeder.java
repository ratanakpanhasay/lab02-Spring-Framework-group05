package edu.itc.shop.service;

import edu.itc.shop.domain.*;
import edu.itc.shop.domain.enums.Category;
import edu.itc.shop.domain.enums.OrderStatus;
import edu.itc.shop.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final TagRepository tags;

    public DataSeeder(CustomerRepository customers, ProductRepository products,
                      OrderRepository orders, TagRepository tags) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
        this.tags = tags;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Tag featured = tags.findByName("dev-featured").orElseGet(() -> tags.save(new Tag("dev-featured")));
        Tag essentials = tags.findByName("dev-essentials").orElseGet(() -> tags.save(new Tag("dev-essentials")));
        String[] names = {"Mechanical Keyboard", "USB Keyboard", "Keycap Set", "Monitor",
                "Mouse", "Headphones", "T-shirt", "Jacket", "Rice", "Coffee", "Java Book", "Notebook"};
        Category[] categories = {Category.HARDWARE, Category.HARDWARE, Category.HARDWARE,
                Category.ELECTRONICS, Category.ELECTRONICS, Category.ELECTRONICS,
                Category.CLOTHING, Category.CLOTHING, Category.FOOD, Category.FOOD, Category.BOOK, Category.OTHER};
        String[] prices = {"55.00", "25.00", "15.00", "180.00", "20.00", "65.00",
                "12.00", "45.00", "8.00", "10.00", "35.00", "5.00"};
        List<Product> catalog = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            String sku = "DEV-%03d".formatted(i + 1);
            Product product = products.findBySku(sku).orElseGet(() -> {
                Product created = new Product(sku, names[index],
                        new Money(new BigDecimal(prices[index]), "USD"), categories[index],
                        index % 4 == 2 ? 0 : 5 + index);
                created.tag(featured);
                created.tag(essentials);
                return products.save(created);
            });
            catalog.add(product);
        }
        for (int c = 0; c < 3; c++) {
            String name = List.of("Dara", "Sophea", "Sokha").get(c);
            String email = name.toLowerCase(java.util.Locale.ROOT) + "@example.com";
            Customer customer = customers.findByEmail(email).orElseGet(() -> customers.save(
                    new Customer(name, email, new Address("123 Main St", "Phnom Penh", "Cambodia", "12000"))));
            // All seed writes are one transaction. Existing customer orders identify a completed batch.
            if (orders.findFirstByCustomerIdOrderByPlacedAtDesc(customer.getId()).isPresent()) {
                continue;
            }
            for (int n = 0; n < 10; n++) {
                int index = c * 10 + n;
                Order order = new Order(customer, OrderStatus.values()[index % OrderStatus.values().length],
                        Instant.parse("2026-01-01T00:00:00Z").plus(index, ChronoUnit.HOURS));
                for (int line = 0; line < 2 + index % 3; line++) {
                    order.addLine(catalog.get((index + line) % catalog.size()), 1 + line % 2);
                }
                orders.save(order);
            }
        }
    }
}
