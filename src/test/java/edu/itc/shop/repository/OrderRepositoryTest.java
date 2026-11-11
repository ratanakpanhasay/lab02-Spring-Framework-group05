package edu.itc.shop.repository;

import edu.itc.shop.domain.*;
import edu.itc.shop.domain.enums.Category;
import edu.itc.shop.domain.enums.OrderStatus;
import edu.itc.shop.web.dto.ProductView;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class OrderRepositoryTest {
    @Autowired EntityManager em;
    @Autowired CustomerRepository customers;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;

    private Customer customer;
    private Product keyboard;
    private Product keycaps;
    private Order older;
    private Order newer;
    private Order pending;

    @BeforeEach
    void setUp() {
        customer = customers.save(new Customer("Dara", "dara@test.example", null));
        keyboard = products.save(new Product("TEST-KEY", "Mechanical Keyboard",
                new Money(new BigDecimal("55.00"), "USD"), Category.HARDWARE, 8));
        keycaps = products.save(new Product("TEST-CAPS", "Keycap Set",
                new Money(new BigDecimal("15.00"), "USD"), Category.HARDWARE, 0));
        products.save(new Product("TEST-BOOK", "Java Book",
                new Money(new BigDecimal("20.00"), "USD"), Category.BOOK, 3));
        products.save(new Product("TEST-USB", "USB Keyboard",
                new Money(new BigDecimal("25.00"), "USD"), Category.HARDWARE, 4));
        products.save(new Product("TEST-PRO", "Pro Keyboard",
                new Money(new BigDecimal("99.00"), "USD"), Category.HARDWARE, 1));
        Tag tag = new Tag("typing");
        em.persist(tag);
        keyboard.tag(tag);
        older = new Order(customer, OrderStatus.PAID, Instant.parse("2026-01-01T00:00:00Z"));
        older.addLine(keyboard, 1);
        older.addLine(keycaps, 2);
        orders.save(older);
        newer = new Order(customer, OrderStatus.PAID, Instant.parse("2026-01-02T00:00:00Z"));
        newer.addLine(keyboard, 2);
        orders.save(newer);
        pending = orders.save(new Order(customer));
        em.flush();
        em.clear();
    }

    @Test
    void derivedQueriesTraverseCustomerAndOrderByPlacedAt() {
        assertThat(customers.findByEmail(customer.getEmail())).get()
                .extracting(Customer::getName).isEqualTo("Dara");
        assertThat(customers.findByEmail("absent@test.example")).isEmpty();
        assertThat(orders.findByCustomerEmailAndStatusOrderByPlacedAtDesc(customer.getEmail(), OrderStatus.PAID))
                .extracting(Order::getId).containsExactly(newer.getId(), older.getId());
        assertThat(orders.countByStatus(OrderStatus.PAID)).isEqualTo(2);
        assertThat(orders.findFirstByCustomerIdOrderByPlacedAtDesc(customer.getId())).get()
                .extracting(Order::getId).isEqualTo(pending.getId());
        assertThat(orders.findFirstByCustomerIdOrderByPlacedAtDesc(-1L)).isEmpty();
    }

    @Test
    void derivedProductQueriesTraverseEmbeddedPriceAndTags() {
        assertThat(products.existsBySku("TEST-KEY")).isTrue();
        assertThat(products.existsBySku("absent")).isFalse();
        assertThat(products.findTop3ByCategoryOrderByPriceAmountDesc(Category.HARDWARE))
                .extracting(Product::getSku).containsExactly("TEST-PRO", "TEST-KEY", "TEST-USB");
        assertThat(products.findByTagsName("typing")).extracting(Product::getId)
                .containsExactly(keyboard.getId());
    }

    @Test
    void fetchJoinInitializesLinesAndProductsAndRetainsEmptyOrders() {
        var fetched = orders.findWithLinesByCustomerEmail(customer.getEmail());
        assertThat(fetched).hasSize(3);
        var loaded = em.getEntityManagerFactory().getPersistenceUnitUtil();
        for (Order order : fetched) {
            assertThat(Hibernate.isInitialized(order.getLines())).isTrue();
            // getLines() returns an unmodifiable wrapper; check the actual mapped collection as well.
            assertThat(loaded.isLoaded(order, "lines")).isTrue();
            assertThat(Hibernate.isInitialized(order.getCustomer())).isTrue();
            order.getLines().forEach(line -> assertThat(Hibernate.isInitialized(line.getProduct())).isTrue());
        }
        em.clear();
        assertThat(fetched.stream().mapToInt(order -> order.getLines().size()).sum()).isEqualTo(3);
        assertThat(orders.findWithLinesByCustomerEmail("missing@test.example")).isEmpty();
    }

    @Test
    void ordinaryLookupKeepsAssociationsLazyInClearedContext() {
        Order ordinary = orders.findById(older.getId()).orElseThrow();
        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(ordinary, "lines")).isFalse();
        assertThat(Hibernate.isInitialized(ordinary.getCustomer())).isFalse();
        em.clear();
        assertThatThrownBy(() -> ordinary.getLines().size()).isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void aggregateCountsLinesRatherThanOrdersOrQuantities() {
        assertThat(orders.countLinesByStatus(OrderStatus.PAID)).isEqualTo(3);
        assertThat(orders.countLinesByStatus(OrderStatus.PENDING)).isZero();
        assertThat(orders.countLinesByStatus(OrderStatus.CANCELLED)).isZero();
    }

    @Test
    void nativeLowStockQueryUsesExclusiveThreshold() {
        assertThat(products.findLowStock(4)).extracting(Product::getStock).containsExactly(0, 1, 3);
        assertThat(products.findLowStock(0)).isEmpty();
    }

    @Test
    void interfaceProjectionIncludesNestedNameAndSupportsPagingAndSorting() {
        var first = orders.findByStatus(OrderStatus.PAID,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "placedAt")));
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).singleElement().satisfies(summary -> {
            assertThat(summary.getId()).isEqualTo(newer.getId());
            assertThat(summary.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(summary.getPlacedAt()).isEqualTo(newer.getPlacedAt());
            assertThat(summary.getCustomer().getName()).isEqualTo("Dara");
        });
        assertThat(orders.findByStatus(OrderStatus.PAID, first.nextPageable()).getContent())
                .singleElement().satisfies(summary -> assertThat(summary.getId()).isEqualTo(older.getId()));
        assertThat(orders.findAllProjectedBy(PageRequest.of(0, 20)).getTotalElements()).isEqualTo(3);
    }

    @Test
    void recordProjectionSelectsEmbeddedAmountAndCountsAllPages() {
        var result = products.findByCategory(Category.HARDWARE,
                PageRequest.of(0, 2, Sort.by("price.amount")));
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).extracting(ProductView::sku).containsExactly("TEST-CAPS", "TEST-USB");
        assertThat(result.getContent().getFirst()).satisfies(view -> {
            assertThat(view.id()).isEqualTo(keycaps.getId());
            assertThat(view.name()).isEqualTo("Keycap Set");
            assertThat(view.amount()).isEqualByComparingTo("15.00");
            assertThat(view.stock()).isZero();
        });
        assertThat(products.findByCategory(Category.FOOD, PageRequest.of(0, 2))).isEmpty();
    }

    @Test
    void specificationsComposeOptionalFiltersCaseInsensitively() {
        var filtered = products.findAll(ProductSpecs.filter("KEY", Category.HARDWARE,
                new BigDecimal("60"), true), PageRequest.of(0, 1, Sort.by("price.amount")));
        assertThat(filtered.getTotalElements()).isEqualTo(2);
        assertThat(filtered.getContent()).extracting(Product::getSku).containsExactly("TEST-USB");
        assertThat(products.findAll(ProductSpecs.filter(null, null, null, null))).hasSize(5);
        assertThat(products.findAll(ProductSpecs.filter("  ", null, null, null))).hasSize(5);
        assertThat(products.findAll(ProductSpecs.filter(null, null, null, false)))
                .extracting(Product::getSku).containsExactly("TEST-CAPS");
        assertThat(products.findAll(ProductSpecs.cheaperThan(new BigDecimal("15")))).isEmpty();
        assertThat(products.findAll(ProductSpecs.inStock())).hasSize(4);
        assertThat(products.findAll(ProductSpecs.filter("absent", null, null, null))).isEmpty();
    }

    @Test
    void nameSearchTreatsSqlWildcardsAsLiterals() {
        products.saveAndFlush(new Product("TEST-LITERAL", "100%_\\ Keyboard",
                new Money(BigDecimal.ONE, "USD"), Category.HARDWARE, 1));
        assertThat(products.findAll(ProductSpecs.nameContains("%_\\")))
                .extracting(Product::getSku).containsExactly("TEST-LITERAL");
    }

    @Test
    void legacyConstructorGeneratesDistinctSkusAndValidStock() {
        Product first = products.save(new Product("Legacy one", new Money(BigDecimal.ONE, "USD"), Category.OTHER));
        Product second = products.saveAndFlush(new Product("Legacy two", new Money(BigDecimal.ONE, "USD"), Category.OTHER));
        assertThat(first.getSku()).startsWith("AUTO-").isNotEqualTo(second.getSku());
        assertThat(first.getStock()).isZero();
        assertThatThrownBy(() -> new Product("NEGATIVE", "Invalid", new Money(BigDecimal.ONE, "USD"),
                Category.OTHER, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Product(" ", "Invalid", new Money(BigDecimal.ONE, "USD"),
                Category.OTHER, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
