
package edu.itc.shop.service;

import edu.itc.shop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

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
