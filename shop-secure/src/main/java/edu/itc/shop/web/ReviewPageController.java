
package edu.itc.shop.web;

import edu.itc.shop.repository.ReviewRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ReviewPageController {

    private final ReviewRepository reviewRepository;

    public ReviewPageController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/products/{id}/reviews.html")
    public String showReviews(
            @PathVariable("id") Long id,
            Model model) {

        model.addAttribute("productId", id);
        model.addAttribute("reviews",
                reviewRepository.findByProductId(id));

        return "reviews";
    }
}
