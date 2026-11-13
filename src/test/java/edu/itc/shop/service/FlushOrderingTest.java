
package edu.itc.shop.service;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import edu.itc.shop.domain.Tag;
import edu.itc.shop.repository.TagRepository;
import jakarta.persistence.EntityManager;

@SpringBootTest
class FlushOrderingTest {

    @Autowired
    private TagRepository tags;

    @Autowired
    private TransactionTemplate tx;

    @Autowired
    private EntityManager entityManager;

    @Test
    void replacingTagWithoutFlushCausesUniqueConstraintViolation() {

        String name = "sale-" + UUID.randomUUID();

        // Create the original tag
        tx.executeWithoutResult(status ->
                tags.saveAndFlush(new Tag(name))
        );

        // Delete and insert with the same name
        assertThatThrownBy(() ->
                tx.executeWithoutResult(status -> {

                    Tag oldTag = tags.findByName(name).orElseThrow();

                    tags.delete(oldTag);

                    tags.save(new Tag(name));

                    // Hibernate attempts INSERT before DELETE
                    entityManager.flush();
                })
        ).isInstanceOf(RuntimeException.class);

        // The failed transaction rolled back.
        assertThat(tags.findByName(name)).isPresent();
    }

    @Test
    void replacingTagWithFlushSucceeds() {

        String name = "sale-" + UUID.randomUUID();

        // Create the original tag
        tx.executeWithoutResult(status ->
                tags.saveAndFlush(new Tag(name))
        );

        // Delete first, flush, then insert replacement
        tx.executeWithoutResult(status -> {

            Tag oldTag = tags.findByName(name).orElseThrow();

            tags.delete(oldTag);

            // Execute DELETE before INSERT
            tags.flush();

            tags.saveAndFlush(new Tag(name));
        });

        // Verify replacement exists
        assertThat(tags.findByName(name)).isPresent();
    }
}
