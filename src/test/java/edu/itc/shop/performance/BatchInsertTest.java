package edu.itc.shop.performance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.enums.Category;

@SpringBootTest
@Transactional
class BatchInsertTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

   @Test
    void demonstrateBatchInsert() {

        SessionFactory sessionFactory =
            entityManagerFactory.unwrap(SessionFactory.class);

        Statistics statistics = sessionFactory.getStatistics();

        statistics.clear();

        long start = System.nanoTime();

        for (int i = 0; i < 1000; i++) {

            Product product = new Product(
                "Batch Product " + i,
                new Money(new BigDecimal("19.99"), "USD"),
                Category.ELECTRONICS
            );

            entityManager.persist(product);

            // Flush periodically to avoid retaining too many entities
            if ((i + 1) % 20 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        long elapsed = System.nanoTime() - start;

        System.out.printf(
            "BATCH INSERT - Execution time: %.3f ms%n",
            elapsed / 1_000_000.0
        );

        System.out.println(
            "BATCH INSERT - Prepared statements: " +
            statistics.getPrepareStatementCount()
        );
        System.out.println(
            "BATCH INSERT - Entities inserted: " +
            statistics.getEntityInsertCount()
        );

        System.out.println(
            "BATCH INSERT - Configured batch size: " +
            sessionFactory.getProperties().get("hibernate.jdbc.batch_size")
        );
    }


}