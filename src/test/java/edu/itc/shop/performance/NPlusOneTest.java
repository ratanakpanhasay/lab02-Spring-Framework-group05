package edu.itc.shop.performance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import edu.itc.shop.repository.OrderRepository;
import java.math.BigDecimal;
import edu.itc.shop.domain.Customer;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.enums.Category;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;

@SpringBootTest
@Transactional
class NPlusOneTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory; 

     @Autowired
    private EntityManager entityManager;

    private double median(double[] times) {
        double[] sorted = times.clone();
        Arrays.sort(sorted);

        int middle = sorted.length / 2;

        if (sorted.length % 2 == 0) {
            return (sorted[middle - 1] + sorted[middle]) / 2.0;
        }

        return sorted[middle];
    }


    @Test
    void demonstrateLazyLoadingWithBatchSize() {

        SessionFactory sessionFactory =
            entityManagerFactory.unwrap(SessionFactory.class);

        Statistics statistics = sessionFactory.getStatistics();

        statistics.clear();

        List<Order> orders = orderRepository.findAll();

        for (Order order : orders) {
            order.getLines().size();
        }

        long statementCount = statistics.getPrepareStatementCount();

        assertEquals(3, orders.size());
        assertEquals(3, statementCount);

        System.out.println(
            "LAZY LOADING - Number of orders: " + orders.size()
        );

        System.out.println(
            "LAZY LOADING - SQL statements executed: " + statementCount
        );
    }
        

// 1 : join fetch 
    @Test
    void demonstrateJoinFetch() {

        SessionFactory sessionFactory =
            entityManagerFactory.unwrap(SessionFactory.class);

        Statistics statistics = sessionFactory.getStatistics();

        double[] times = new double[5];

        // 2 warm-up runs + 5 measured runs
        for (int i = 0; i < 7; i++) {

            // Remove previously loaded entities from memory
            entityManager.clear();

            // Reset SQL statement counter
            statistics.clear();

            long start = System.nanoTime();

            List<Order> orders = orderRepository.findAllWithLines();

            for (Order order : orders) {
                order.getLines().size();
            }

            long elapsed = System.nanoTime() - start;

            assertEquals(3, orders.size());
            assertEquals(1, statistics.getPrepareStatementCount());

            // Ignore first 2 warm-up runs
            if (i >= 2) {
                times[i - 2] = elapsed / 1_000_000.0;
            }
        }

        System.out.printf(
            "JOIN FETCH - Median execution time: %.3f ms%n",
            median(times)
        );

        System.out.println("JOIN FETCH - SQL statements: 1");
    }

    @BeforeEach
    void setUp() {
        Customer customer = new Customer(
            "Performance Customer",
            "performance@example.com",
            null
        );

        Product product = new Product(
            "Test Laptop",
            new Money(new BigDecimal("500.00"), "USD"),
            Category.ELECTRONICS
        );

        entityManager.persist(customer);
        entityManager.persist(product);

        for (int i = 0; i < 3; i++) {
            Order order = new Order(customer);
            order.addLine(product, 1);
            entityManager.persist(order);
        }

        entityManager.flush();
        entityManager.clear();
    }

    // 2: entity graph

    @Test
    void demonstrateEntityGraph() {

        SessionFactory sessionFactory =
            entityManagerFactory.unwrap(SessionFactory.class);

        Statistics statistics = sessionFactory.getStatistics();

        double[] times = new double[5];

        // 2 warm-up runs + 5 measured runs
        for (int i = 0; i < 7; i++) {

            entityManager.clear();
            statistics.clear();

            long start = System.nanoTime();

            List<Order> orders = orderRepository.findAllWithLinesGraph();

            for (Order order : orders) {
                order.getLines().size();
            }

            long elapsed = System.nanoTime() - start;

            assertEquals(3, orders.size());
            assertEquals(1, statistics.getPrepareStatementCount());

            if (i >= 2) {
                times[i - 2] = elapsed / 1_000_000.0;
            }
        }

        System.out.printf(
            "ENTITY GRAPH - Median execution time: %.3f ms%n",
            median(times)
        );

        System.out.println("ENTITY GRAPH - SQL statements: 1");
    }

    @Test
    void demonstrateBatchSize() {

        SessionFactory sessionFactory =
            entityManagerFactory.unwrap(SessionFactory.class);

        Statistics statistics = sessionFactory.getStatistics();

        double[] times = new double[5];

        // 2 warm-up runs + 5 measured runs
        for (int i = 0; i < 7; i++) {

            entityManager.clear();
            statistics.clear();

            long start = System.nanoTime();

            List<Order> orders = orderRepository.findAll();

            for (Order order : orders) {
                order.getLines().size();
            }

            long elapsed = System.nanoTime() - start;

            assertEquals(3, orders.size());
            assertEquals(3, statistics.getPrepareStatementCount());

            if (i >= 2) {
                times[i - 2] = elapsed / 1_000_000.0;
            }
        }

        System.out.printf(
            "BATCH SIZE - Median execution time: %.3f ms%n",
            median(times)
        );

        System.out.println("BATCH SIZE - SQL statements: 3");
    }
        
}