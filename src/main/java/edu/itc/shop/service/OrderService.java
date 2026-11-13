
package edu.itc.shop.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.enums.OrderStatus;
import edu.itc.shop.repository.OrderRepository;
import edu.itc.shop.web.dto.MoneyView;
import edu.itc.shop.web.dto.OrderDetail;
import edu.itc.shop.web.dto.OrderDto;
import edu.itc.shop.web.dto.OrderLineView;
import edu.itc.shop.web.dto.OrderSummary;
import edu.itc.shop.web.dto.OrderSummaryResponse;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    // Task 4: Managed entity and dirty checking
    @Transactional
    public void markPaid(Long orderId) {

        Order order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + orderId));

        order.changeStatus(OrderStatus.PAID);

        // No save() required.
        // Hibernate automatically detects the change
        // because the order is a managed entity.
    }

    // Task 4: Change order status for optimistic locking
    @Transactional
    public void changeStatus(Long id, OrderStatus next) {

        Order order = orders.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + id));

        order.changeStatus(next);

        // No save() required.
        // @Version detects concurrent modifications.
    }

    // Existing Task 3: List orders
    public Page<OrderSummary> list(
            OrderStatus status, Pageable pageable) {

        Page<OrderSummary> result = status == null
                ? orders.findAllProjectedBy(pageable)
                : orders.findByStatus(status, pageable);

        return result.map(summary ->
                new OrderSummaryResponse(
                        summary.getId(),
                        summary.getStatus(),
                        summary.getPlacedAt(),
                        new OrderSummaryResponse.Name(
                                summary.getCustomer().getName())
                )
        );
    }

    // Existing Task 3: Retrieve customer orders
    @Transactional(readOnly = true)
    public List<OrderDetail> customerOrders(String email) {

        return orders.findWithLinesByCustomerEmail(email)
                .stream()
                .map(this::detail)
                .toList();
    }

    private OrderDetail detail(Order order) {

        List<OrderLineView> lines = order.getLines()
                .stream()
                .map(line ->
                        new OrderLineView(
                                line.getId(),
                                line.getProduct().getId(),
                                line.getProduct().getSku(),
                                line.getProduct().getName(),
                                line.getQuantity(),
                                money(line.getUnitPrice())
                        )
                )
                .toList();

        return new OrderDetail(
                order.getId(),
                order.getStatus(),
                order.getPlacedAt(),
                order.getCustomer().getName(),
                order.getCustomer().getEmail(),
                lines,
                money(order.total())
        );
    }

    private MoneyView money(Money value) {
        return new MoneyView(
                value.amount(),
                value.currency()
        );
    }


// Task 4: Demonstrate read-only transaction
@Transactional(readOnly = true)
public OrderDto view(Long id) {

    Order order = orders.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                    "Order not found: " + id));

    // Deliberately attempt to modify the managed entity.
    order.changeStatus(OrderStatus.CANCELLED);

    return new OrderDto(
            order.getId(),
            order.getStatus(),
            order.getPlacedAt()
    );
}

}
