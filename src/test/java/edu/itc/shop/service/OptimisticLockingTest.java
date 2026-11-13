
package edu.itc.shop.service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.enums.OrderStatus;
import edu.itc.shop.repository.OrderRepository;

@SpringBootTest
class OptimisticLockingTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private TransactionTemplate tx;

    @Autowired
    private TestData data;

    @Test
    void secondWriterFailsWithOptimisticLock() throws Exception {

        Long orderId = data.newOrder();

        long versionBefore = orders.findById(orderId)
                .orElseThrow()
                .getVersion();

        CountDownLatch bothLoaded = new CountDownLatch(2);

        Function<OrderStatus, Callable<Throwable>> writer = target -> () -> {

            try {
                tx.executeWithoutResult(status -> {

                    Order order = orders.findById(orderId)
                            .orElseThrow();

                    bothLoaded.countDown();
                    await(bothLoaded);

                    order.changeStatus(target);
                });

                return null;

            } catch (RuntimeException e) {
                return e;
            }
        };

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {

            List<Future<Throwable>> results = pool.invokeAll(
                    List.of(
                            writer.apply(OrderStatus.PAID),
                            writer.apply(OrderStatus.CANCELLED)
                    )
            );

            List<Throwable> failures = results.stream()
                    .map(this::get)
                    .filter(Objects::nonNull)
                    .toList();

            assertThat(failures).hasSize(1);

            assertThat(failures.get(0))
                    .isInstanceOf(
                            ObjectOptimisticLockingFailureException.class
                    );

            Order after = orders.findById(orderId).orElseThrow();

            assertThat(after.getVersion())
                    .isEqualTo(versionBefore + 1);

            assertThat(after.getStatus())
                    .isIn(OrderStatus.PAID, OrderStatus.CANCELLED);
        }
    }

    private static void await(CountDownLatch latch) {

        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Latch timeout");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(e);
        }
    }

    private Throwable get(Future<Throwable> future) {

        try {
            return future.get();

        } catch (Exception e) {
            return e;
        }
    }
}
