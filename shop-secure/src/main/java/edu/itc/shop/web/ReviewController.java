
package edu.itc.shop.web;

import edu.itc.shop.domain.Review;
import edu.itc.shop.dto.ReviewRequest;
import edu.itc.shop.repository.ReviewRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewRepository reviewRepository;

    public ReviewController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public Review createReview(
            @Valid @RequestBody ReviewRequest request) {

        Review review = new Review(
                request.productId(),
                request.stars(),
                request.text()
        );

        return reviewRepository.save(review);
    }

    @GetMapping("/products/{id}/reviews")
    public List<Review> getReviews(@PathVariable("id") Long id) {
        return reviewRepository.findByProductId(id);
    }
}
