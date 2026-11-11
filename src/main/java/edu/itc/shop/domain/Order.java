
package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.itc.shop.domain.enums.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "orders_seq"
    )
    @SequenceGenerator(
        name = "orders_seq",
        sequenceName = "order_seq",
        allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt = Instant.now();

    @Version
    private long version;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
    }

    public Order(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException(
                "Customer cannot be null"
            );
        }

        this.customer = customer;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public void addLine(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException(
                "Product cannot be null"
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                "Quantity must be positive"
            );
        }

        OrderLine line = new OrderLine(
            this,
            product,
            quantity,
            product.getPrice()
        );

        lines.add(line);
    }

    public void removeLine(OrderLine line) {
        if (lines.remove(line)) {
            line.setOrder(null);
        }
    }

    public Money total() {
        if (lines.isEmpty()) {
            return new Money(BigDecimal.ZERO, "USD");
        }

        String currency = lines.get(0).getUnitPrice().currency();
        BigDecimal amount = BigDecimal.ZERO;

        for (OrderLine line : lines) {
            Money unitPrice = line.getUnitPrice();

            if (!currency.equals(unitPrice.currency())) {
                throw new IllegalStateException(
                    "Cannot calculate total with different currencies"
                );
            }

            BigDecimal lineTotal = unitPrice.amount().multiply(
                BigDecimal.valueOf(line.getQuantity())
            );

            amount = amount.add(lineTotal);
        }

        return new Money(amount, currency);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Order order = (Order) obj;

        return id != null && id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", status=" + status +
                ", placedAt=" + placedAt +
                '}';
    }
}



