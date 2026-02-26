package com.example.demo.Service;

import com.example.demo.DTOs.ReviewResponse;
import com.example.demo.Model.Booking;
import com.example.demo.Model.Review;
import com.example.demo.Repository.BookingRepository;
import com.example.demo.Repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public Review addReview(Long customerId, Review review) {
        Booking booking = bookingRepository.findById(review.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + review.getBookingId()));

        if (!booking.getCustomerId().equals(customerId)) {
            throw new RuntimeException("You can only review your own bookings");
        }
        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new RuntimeException("You can only review completed bookings");
        }
        reviewRepository.findByBookingIdAndCustomerId(review.getBookingId(), customerId)
                .ifPresent(r -> { throw new RuntimeException("You have already reviewed this booking"); });

        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        review.setCustomerId(customerId);
        review.setCarId(booking.getCarId());
        reviewRepository.save(review);

        return reviewRepository.findByBookingIdAndCustomerId(review.getBookingId(), customerId)
                .orElseThrow(() -> new RuntimeException("Review creation failed"));
    }

    public List<ReviewResponse> getAllReviews(int page, int size) {
        return reviewRepository.findAll(page, size);
    }

    public List<Review> getReviewsByCarId(Long carId, int page, int size) {
        return reviewRepository.findByCarId(carId, page, size);
    }

    public Double getAverageRating(Long carId) {
        return reviewRepository.getAverageRatingByCarId(carId);
    }
}