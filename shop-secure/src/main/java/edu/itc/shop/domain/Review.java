
package edu.itc.shop.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int stars;

    @Column(nullable = false, length = 1000)
    private String text;

    protected Review() {
    }

    public Review(Long productId, int stars, String text) {
        this.productId = productId;
        this.stars = stars;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public int getStars() {
        return stars;
    }

    public String getText() {
        return text;
    }
}
