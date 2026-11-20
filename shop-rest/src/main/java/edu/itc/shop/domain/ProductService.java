package edu.itc.shop.domain;

import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ClientErrorException;


public class ProductService {
    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    // Get all products
    public List<Product> findAll() {
        return repo.findAll();
    }

    // Get product by ID – throws 404 if missing
    public Product get(long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }

    // Create a new product
    public Product create(Product product) {
        repo.findBySku(product.sku()).ifPresent(existing -> {
            throw new DuplicateSkuException(product.sku());
        });
        return repo.save(product);
    }

    // Replace an existing product (PUT)
    public Product update(long id, Product product) {
        Product existing = get(id); // 404 if missing

        repo.findBySku(product.sku()).ifPresent(found -> {
            if (found.id() != id) {
                throw new DuplicateSkuException(product.sku());
            }
        });

        Product updated = new Product(
                id,
                product.name(),
                product.sku(),
                product.price(),
                product.stock(),
                existing.version()
        );

        return repo.save(updated);
    }


    public Product updateIfVersionMatches(
            long id,
            Product product,
            int expectedVersion) {

        // Check that the product exists (404 if missing)
        get(id);

        // Check whether another product already uses this SKU
        repo.findBySku(product.sku()).ifPresent(found -> {
            if (found.id() != id) {
                throw new DuplicateSkuException(product.sku());
            }
        });

        // Perform the version check and update atomically
        return repo.updateIfVersionMatches(
                product,
                expectedVersion
        ).orElseThrow(() ->
                new ClientErrorException(
                        "Product version does not match",
                        412
                )
        );
    }


    // Adjust stock by delta (PATCH /stock)
    public Product changeStock(long id, int delta) {
        Product existing = get(id); // 404 if missing

        int newStock = existing.stock() + delta;

        if (newStock < 0) {
            throw new InsufficientStockException();
        }

        Product updated = new Product(
                existing.id(),
                existing.name(),
                existing.sku(),
                existing.price(),
                newStock,
                existing.version()
        );

        return repo.save(updated);
    }

    // Delete a product (DELETE)
    public void delete(long id) {
        get(id); // 404 if missing
        repo.delete(id);
    }
}
