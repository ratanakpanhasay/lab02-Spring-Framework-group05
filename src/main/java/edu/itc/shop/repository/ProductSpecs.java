package edu.itc.shop.repository;

import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.enums.Category;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecs {
    private ProductSpecs() {}

    public static Specification<Product> nameContains(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return null;
            }
            String literal = q.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            return cb.like(cb.lower(root.get("name")), "%" + literal + "%", '\\');
        };
    }

    public static Specification<Product> inCategory(Category category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Product> cheaperThan(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null ? null
                : cb.lessThan(root.get("price").get("amount"), maxPrice);
    }

    public static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("stock"), 0);
    }

    public static Specification<Product> filter(String q, Category category, BigDecimal maxPrice, Boolean inStock) {
        Specification<Product> stockFilter = inStock == null ? Specification.unrestricted()
                : inStock ? inStock() : (root, query, cb) -> cb.equal(root.get("stock"), 0);
        return Specification.allOf(nameContains(q), inCategory(category), cheaperThan(maxPrice), stockFilter);
    }
}
