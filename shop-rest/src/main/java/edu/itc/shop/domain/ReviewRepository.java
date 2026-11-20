
package edu.itc.shop.domain;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ReviewRepository {

    private final ConcurrentHashMap<Long, Review> reviews =
            new ConcurrentHashMap<>();

    private final AtomicLong nextId = new AtomicLong(1);

    // Get all reviews belonging to one product
    public List<Review> findByProductId(long productId) {
        return reviews.values().stream()
                .filter(review -> review.productId() == productId)
                .sorted(Comparator.comparingLong(Review::id))
                .toList();
    }

    // Save a new review and generate its ID
    public Review save(Review review) {
        long id = nextId.getAndIncrement();

        Review saved = new Review(
                id,
                review.productId(),
                review.stars(),
                review.author(),
                review.text()
        );

        reviews.put(id, saved);

        return saved;
    }
}
