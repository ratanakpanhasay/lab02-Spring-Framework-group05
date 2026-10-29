
package edu.itc.shop.service;

import java.util.List;

import edu.itc.shop.domain.Product;
import edu.itc.shop.repository.ProductRepository;
import edu.itc.shop.web.dto.CreateProduct;
import edu.itc.shop.web.dto.ProductDto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }


    //search by name
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

    //find by id
    public ProductDto findById(long id) {
        return products.findById(id)
                .map(ProductDto::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
                    
    }

    //CRUD------
    // create product 
    public ProductDto create(CreateProduct request) {
        Product product = new Product(
                null,
                request.name(),
                request.price(),
                request.stock()
        );

        long id = products.insert(product); //insert return the id 

        return findById(id);
    }

    //update stock
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
    //delete
    
    public void delete(long id) {
        int deleted = products.delete(id);

        if (deleted == 0) {
            throw new ProductNotFoundException(id);
        }
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
