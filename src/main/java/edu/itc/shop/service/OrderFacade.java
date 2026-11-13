
package edu.itc.shop.service;

import edu.itc.shop.domain.enums.OrderStatus;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class OrderFacade {

    private final OrderService orderService;

    public OrderFacade(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Retries the whole transaction up to 3 times.
     */
    public void changeStatusWithRetry(Long id, OrderStatus next) {

        for (int attempt = 1; ; attempt++) {

            try {
                orderService.changeStatus(id, next);
                return;

            } catch (ObjectOptimisticLockingFailureException e) {

                if (attempt == 3) {
                    throw e;
                }

                try {
                    Thread.sleep(25L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Retry interrupted", interrupted
                    );
                }
            }
        }
    }
}
