package edu.itc.shop.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import edu.itc.shop.domain.enums.Category;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

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

    @Column(name = "sku", length = 64, nullable = false, unique = true)
    private String sku;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(
            name = "amount",
            column = @Column(name = "price_amount")
        ),
        @AttributeOverride(
            name = "currency",
            column = @Column(name = "price_currency")
        )
    })
    private Money price;

    @Enumerated(EnumType.STRING)
    private Category category;

    @ManyToMany
    @JoinTable(
        name = "product_tag",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    protected Product() {
    }

    public Product(String name, Money price, Category category) {
        this("AUTO-" + UUID.randomUUID(), name, price, category, 0);
    }

    public Product(String sku, String name, Money price, Category category, int stock) {
        if (sku == null || sku.isBlank() || sku.length() > 64) {
            throw new IllegalArgumentException("SKU must contain 1-64 characters");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.sku = sku;
        this.stock = stock;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public int getStock() {
        return stock;
    }

    public Money getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public void tag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }

        if (tags.add(tag)) {
            tag.addProduct(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Product product = (Product) obj;

        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", category=" + category +
                '}';
    }
}

