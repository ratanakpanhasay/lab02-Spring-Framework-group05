
package edu.itc.shop.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.enums.OrderStatus;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderFacade orderFacade;

    @Test
    void retriesUntilThirdAttemptSucceeds() {

        ObjectOptimisticLockingFailureException conflict =
                new ObjectOptimisticLockingFailureException(
                        Order.class, 1L);

        doThrow(conflict)
                .doThrow(conflict)
                .doNothing()
                .when(orderService)
                .changeStatus(1L, OrderStatus.PAID);

        orderFacade.changeStatusWithRetry(1L, OrderStatus.PAID);

        verify(orderService, times(3))
                .changeStatus(1L, OrderStatus.PAID);
    }

    @Test
    void stopsAfterThreeFailedAttempts() {

        ObjectOptimisticLockingFailureException conflict =
                new ObjectOptimisticLockingFailureException(
                        Order.class, 1L);

        doThrow(conflict)
                .when(orderService)
                .changeStatus(1L, OrderStatus.PAID);

        assertThatThrownBy(() ->
                orderFacade.changeStatusWithRetry(1L, OrderStatus.PAID))
                .isInstanceOf(
                        ObjectOptimisticLockingFailureException.class);

        verify(orderService, times(3))
                .changeStatus(1L, OrderStatus.PAID);
    }
}
