package edu.itc.shop.web;

import edu.itc.shop.domain.enums.OrderStatus;
import edu.itc.shop.service.OrderService;
import edu.itc.shop.web.dto.OrderSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public Page<OrderSummary> list(@RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orders.list(status, pageable);
    }
}
