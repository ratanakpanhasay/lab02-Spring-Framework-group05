package edu.itc.shop.web;

import edu.itc.shop.domain.Order;
import edu.itc.shop.service.OrderService;
import edu.itc.shop.web.dto.PlaceOrder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Order> place(Authentication auth, @Valid @RequestBody PlaceOrder cmd) {
        Order order = service.place(auth.getName(), cmd);
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId())).body(order);
    }

    @GetMapping
    public List<Order> list(
            Authentication auth,
            @RequestParam(required = false) String customer,
            @RequestParam(defaultValue = "false") boolean all) {
        if (all) {
            return service.all();
        }
        return service.history(customer == null ? auth.getName() : customer);
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/ship")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ship(@PathVariable long id) {
        service.ship(id);
    }
}
