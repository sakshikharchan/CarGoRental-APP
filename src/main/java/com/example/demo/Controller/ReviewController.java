package com.example.demo.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.DTOs.ReviewResponse;
import com.example.demo.Model.Review;
import com.example.demo.Service.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<ApiResponse<Review>> addReview(
            @RequestBody Review review,
            Authentication auth) {
        try {
            Long customerId = (Long) auth.getCredentials();
            return ResponseEntity.ok(ApiResponse.success("Review submitted",
                    reviewService.addReview(customerId, review)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getAllReviews(page, size)));
    }

    @GetMapping("/car/{carId}")
    public ResponseEntity<ApiResponse<List<Review>>> getReviewsByCarId(
            @PathVariable Long carId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getReviewsByCarId(carId, page, size)));
    }

    @GetMapping("/car/{carId}/rating")
    public ResponseEntity<ApiResponse<Double>> getAverageRating(@PathVariable Long carId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getAverageRating(carId)));
    }
}