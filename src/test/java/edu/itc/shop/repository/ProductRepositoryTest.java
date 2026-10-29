
package edu.itc.shop.repository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

import edu.itc.shop.domain.Product;

@JdbcTest
@Import(ProductRepository.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Test
    void findsAllFiveSeedProducts() {
        assertEquals(5, repository.findAll().size());
    }

    @Test
    void findsProductById() {
        Product product = repository.findById(1L).orElseThrow();

        assertEquals("Wireless Mouse", product.name());
        assertEquals(25, product.stock());
    }

    @Test
    void searchesProductNamesWithoutCaseSensitivity() {
        var results = repository.findByNameContaining("KEYBOARD");

        assertEquals(1, results.size());
        assertEquals("Mechanical Keyboard", results.get(0).name());
    }

    @Test
    void insertsAndDeletesProduct() {
        Product product = new Product(
                null, "Temporary Adapter", new BigDecimal("12.50"), 7);

        long id = repository.insert(product);

        assertTrue(id > 0);
        assertEquals(
                "Temporary Adapter",
                repository.findById(id).orElseThrow().name()
        );

        assertEquals(1, repository.delete(id));
        assertTrue(repository.findById(id).isEmpty());
    }

    @Test
    void updatesStockWithoutAllowingNegativeValues() {
        assertEquals(1, repository.updateStock(1L, -5));
        assertEquals(20, repository.findById(1L).orElseThrow().stock());

        assertEquals(0, repository.updateStock(1L, -1000));
        assertEquals(20, repository.findById(1L).orElseThrow().stock());
    }

    @Test
    void duplicateNameThrowsDuplicateKeyException() {
        Product duplicate = new Product(
                null, "Wireless Mouse", new BigDecimal("10.00"), 3);

        assertThrows(
                DuplicateKeyException.class,
                () -> repository.insert(duplicate)
        );
    }
}
