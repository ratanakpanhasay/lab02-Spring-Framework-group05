package edu.itc.shop.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "customer_seq_gen"
    )
    @SequenceGenerator(
        name = "customer_seq_gen",
        sequenceName = "customer_seq",
        allocationSize = 50
    )
    private Long id;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Embedded
    private Address address;

    protected Customer() {
    }

    public Customer(String name, String email, Address address) {
        this.name = name;
        this.email = email;
        this.address = address;
    }

    public Long getId()         { return id; }
    public String getName()     { return name; }
    public String getEmail()    { return email; }
    public Address getAddress() { return address; }

}
