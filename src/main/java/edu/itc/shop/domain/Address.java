package edu.itc.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Address(
    String street,
    String city,
    String country,
    @Column(name = "postal_code") String postalCode
) {}

