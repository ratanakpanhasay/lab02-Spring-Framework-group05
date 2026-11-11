package edu.itc.shop.repository;

import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.enums.Category;
import edu.itc.shop.web.dto.ProductView;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsBySku(String sku);

    Optional<Product> findBySku(String sku);

    List<Product> findTop3ByCategoryOrderByPriceAmountDesc(Category category);

    List<Product> findByTagsName(String name);

    @Query(value = "SELECT * FROM product WHERE stock < :min ORDER BY stock, id", nativeQuery = true)
    List<Product> findLowStock(@Param("min") int min);

    @Query(value = """
            select new edu.itc.shop.web.dto.ProductView(p.id, p.sku, p.name, p.price.amount, p.stock)
            from Product p where p.category = :category
            """, countQuery = "select count(p) from Product p where p.category = :category")
    Page<ProductView> findByCategory(@Param("category") Category category, Pageable pageable);
}
