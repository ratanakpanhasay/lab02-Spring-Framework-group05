package edu.itc.shop.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import edu.itc.shop.domain.enums.Category;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "product_seq_gen"
    )
    @SequenceGenerator(
        name = "product_seq_gen",
        sequenceName = "product_seq",
        allocationSize = 50
    )
    private Long id;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount",   column = @Column(name = "price_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "price_currency"))
    })
    private Money price;

    @Enumerated(EnumType.STRING)
    private Category category;

    protected Product() {
    }

    public Product(String name, Money price, Category category) {
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public Long getId()       { return id; }
    public String getName()   { return name; }
    public Money getPrice()   { return price; }
    public Category getCategory() { return category; }

}
