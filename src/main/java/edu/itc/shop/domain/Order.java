package edu.itc.shop.domain;

import edu.itc.shop.domain.enums.OrderStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity 
@Table (name = "orders")
public class Order {
    

    @Id 
    @GeneratedValue (
        strategy = GenerationType.SEQUENCE,
        generator = "order_seq_gen"
    )

    @SequenceGenerator(
        name = "order_seq_gen",
        sequenceName = "order_seq",
        allocationSize = 50
    )

    private Long id;

    @Enumerated (EnumType.STRING)
    private OrderStatus status;

    protected Order() {
        
    }
}
