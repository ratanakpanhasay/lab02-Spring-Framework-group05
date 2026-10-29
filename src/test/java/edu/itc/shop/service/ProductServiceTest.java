
package edu.itc.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.itc.shop.repository.ProductRepository;

@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService service;

    @Autowired
    private ProductRepository repository;

    @Test
    void restockRollsBackWhenSourceHasTooLittleStock() {
        int before1 = repository.findById(1L).orElseThrow().stock();
        int before2 = repository.findById(2L).orElseThrow().stock();

        assertThrows(
                IllegalStateException.class,
                () -> service.restock(1L, 2L, 1000)
        );

        assertEquals(before1,
                repository.findById(1L).orElseThrow().stock());
        assertEquals(before2,
                repository.findById(2L).orElseThrow().stock());
    }

    @Test
    void restockRollsBackWhenDestinationDoesNotExist() {
        int sourceBefore =
                repository.findById(1L).orElseThrow().stock();

        assertThrows(
                IllegalStateException.class,
                () -> service.restock(1L, 999L, 5)
        );

        assertEquals(sourceBefore,
                repository.findById(1L).orElseThrow().stock());
    }
}
