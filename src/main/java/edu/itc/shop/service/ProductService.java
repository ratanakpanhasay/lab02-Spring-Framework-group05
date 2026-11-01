
package edu.itc.shop.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Product;
import edu.itc.shop.repository.ProductRepository;
import edu.itc.shop.web.dto.CreateProduct;
import edu.itc.shop.web.dto.ProductDto;

@Service
public class ProductService {

    private static final Logger log =
            LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    // Search by name
    public List<ProductDto> search(String q) {
        if (q == null || q.isBlank()) {
            return products.findAll()
                    .stream()
                    .map(ProductDto::from)
                    .toList();
        }

        return products.findByNameContaining(q)
                .stream()
                .map(ProductDto::from)
                .toList();
    }

    // Find by ID
    public ProductDto findById(long id) {
        return products.findById(id)
                .map(ProductDto::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    // CRUD

    // Create product
    public ProductDto create(CreateProduct request) {
        Product product = new Product(
                null,
                request.name(),
                request.price(),
                request.stock()
        );

        long id = products.insert(product);

        ProductDto created = findById(id);

        log.info("Product created: id={}, name={}",
                id, request.name());

        return created;
    }

    // Update stock
    public ProductDto updateStock(long id, int delta) {
        int updated = products.updateStock(id, delta);

        if (updated == 0) {
            if (products.findById(id).isEmpty()) {
                throw new ProductNotFoundException(id);
            }

            throw new InsufficientStockException(id);
        }

        return findById(id);
    }

    // Delete product
    public void delete(long id) {
        int deleted = products.delete(id);

        if (deleted == 0) {
            throw new ProductNotFoundException(id);
        }
    }

    // Restock products
    @Transactional
    public void restock(long fromId, long toId, int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero");
        }

        if (fromId == toId) {
            throw new IllegalArgumentException(
                    "Source and destination products must be different");
        }

        // Step 1: Remove stock from the source product.
        int removed = products.updateStock(fromId, -qty);

        if (removed == 0) {
            throw new IllegalStateException(
                    "Source product not found or insufficient stock: "
                            + fromId);
        }

        // Step 2: Add stock to the destination product.
        int added = products.updateStock(toId, qty);

        if (added == 0) {
            throw new IllegalStateException(
                    "Destination product not found: " + toId);
        }
    }
}
