package edu.itc.shop.service;

import edu.itc.shop.domain.enums.Category;
import edu.itc.shop.repository.ProductRepository;
import edu.itc.shop.repository.ProductSpecs;
import edu.itc.shop.web.dto.ProductView;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public Page<ProductView> search(String q, Category category, BigDecimal maxPrice,
                                    Boolean inStock, Pageable pageable) {
        return products.findAll(ProductSpecs.filter(q, category, maxPrice, inStock), pageable)
                .map(p -> new ProductView(p.getId(), p.getSku(), p.getName(), p.getPrice().amount(), p.getStock()));
    }
}
