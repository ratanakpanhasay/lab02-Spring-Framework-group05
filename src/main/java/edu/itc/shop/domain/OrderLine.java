package edu.itc.shop.domain;

import jakarta.persistence.Table;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Column;

@Entity 
@Table (name = "order_line")

public class OrderLine {
    

    @Id 
    @GeneratedValue (
        strategy = GenerationType.SEQUENCE,
        generator = "order_line_seq_gen"
    )
     @SequenceGenerator(
        name = "order_line_seq_gen",
        sequenceName = "order_line_seq",
        allocationSize = 50
    )
    private Long id;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    protected OrderLine() {
    }
}
