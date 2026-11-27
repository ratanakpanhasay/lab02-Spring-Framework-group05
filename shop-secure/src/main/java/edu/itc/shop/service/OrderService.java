package edu.itc.shop.service;

import edu.itc.shop.domain.Order;
import edu.itc.shop.repository.OrderRepository;
import edu.itc.shop.web.dto.PlaceOrder;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @PreAuthorize("#customerId == authentication.name")
    public Order place(String customerId, PlaceOrder cmd) {
        return repository.save(new Order(customerId, cmd.productId(), cmd.quantity()));
    }

    @PreAuthorize("hasRole('ADMIN') or #customerId == authentication.name")
    public List<Order> history(String customerId) {
        return repository.findByCustomer(customerId);
    }

    @PostAuthorize("hasRole('ADMIN') or returnObject.customer == authentication.name")
    public Order get(long id) {
        return findOrder(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void ship(long id) {
        findOrder(id).ship();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Order> all() {
        return repository.findAll();
    }

    private Order findOrder(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order " + id + " not found"));
    }
}
