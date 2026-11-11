package edu.itc.shop.web;

import edu.itc.shop.service.OrderService;
import edu.itc.shop.web.dto.OrderDetail;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final OrderService orders;

    public CustomerController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/{email}/orders")
    public List<OrderDetail> orders(@PathVariable String email) {
        return orders.customerOrders(email);
    }
}
