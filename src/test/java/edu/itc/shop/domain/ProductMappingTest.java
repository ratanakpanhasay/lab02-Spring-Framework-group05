package edu.itc.shop.domain;

import edu.itc.shop.domain.enums.Category;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductMappingTest {

    @Autowired
    EntityManager em;

    // Test 1: Embedded Money values are stored and loaded correctly
    @Test
    void embeddedMoneyIsPersisted() {
        Product p = new Product("Laptop", new Money(new BigDecimal("999.99"), "USD"), Category.ELECTRONICS);
        em.persist(p);
        em.flush();
        em.clear();

        Product found = em.find(Product.class, p.getId());
        assertThat(found.getPrice().amount()).isEqualByComparingTo("999.99");
        assertThat(found.getPrice().currency()).isEqualTo("USD");
    }

    // Test 2: Category enum is stored as STRING (not number)
    @Test
    void enumCategoryIsSavedAsString() {
        Product p = new Product("Book", new Money(new BigDecimal("15.00"), "USD"), Category.BOOK);
        em.persist(p);
        em.flush();

        String raw = (String) em.createNativeQuery(
                "SELECT category FROM product WHERE id = :id")
                .setParameter("id", p.getId())
                .getSingleResult();

        assertThat(raw).isEqualTo("BOOK");
    }

    // Test 3: Embedded Address fields are stored in customer table columns
    @Test
    void embeddedAddressIsPersisted() {
        Address addr = new Address("123 Main St", "Phnom Penh", "Cambodia", "12000");
        Customer c = new Customer("Sreysoth", "srey@itc.edu", addr);
        em.persist(c);
        em.flush();
        em.clear();

        Customer found = em.find(Customer.class, c.getId());
        assertThat(found.getAddress().city()).isEqualTo("Phnom Penh");
        assertThat(found.getAddress().country()).isEqualTo("Cambodia");
    }
}

