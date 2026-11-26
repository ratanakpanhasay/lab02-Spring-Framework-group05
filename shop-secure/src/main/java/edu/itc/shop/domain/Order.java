package edu.itc.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    public enum Status {
        NEW, SHIPPED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String customer;

    @Column(nullable = false)
    private long productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    protected Order() {
        // Required by JPA
    }

    public Order(String customer, long productId, int quantity) {
        this.customer = customer;
        this.productId = productId;
        this.quantity = quantity;
        this.status = Status.NEW;
    }

    public Long getId() {
        return id;
    }

    public String getCustomer() {
        return customer;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Status getStatus() {
        return status;
    }

    public void ship() {
        status = Status.SHIPPED;
    }
}
