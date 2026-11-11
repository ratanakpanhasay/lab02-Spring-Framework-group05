package edu.itc.shop.service;

import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.enums.OrderStatus;
import edu.itc.shop.repository.OrderRepository;
import edu.itc.shop.web.dto.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    public Page<OrderSummary> list(OrderStatus status, Pageable pageable) {
        Page<OrderSummary> result = status == null ? orders.findAllProjectedBy(pageable)
                : orders.findByStatus(status, pageable);
        return result.map(summary -> new OrderSummaryResponse(summary.getId(), summary.getStatus(),
                summary.getPlacedAt(), new OrderSummaryResponse.Name(summary.getCustomer().getName())));
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> customerOrders(String email) {
        return orders.findWithLinesByCustomerEmail(email).stream().map(this::detail).toList();
    }

    private OrderDetail detail(Order order) {
        List<OrderLineView> lines = order.getLines().stream()
                .map(line -> new OrderLineView(line.getId(), line.getProduct().getId(),
                        line.getProduct().getSku(), line.getProduct().getName(),
                        line.getQuantity(), money(line.getUnitPrice())))
                .toList();
        return new OrderDetail(order.getId(), order.getStatus(), order.getPlacedAt(),
                order.getCustomer().getName(), order.getCustomer().getEmail(), lines, money(order.total()));
    }

    private MoneyView money(Money value) {
        return new MoneyView(value.amount(), value.currency());
    }
}
