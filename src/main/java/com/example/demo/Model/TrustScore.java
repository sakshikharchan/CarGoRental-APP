package com.example.demo.Model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrustScore {

    private Long id;
    private Long userId;

    // Component scores (0–100 each)
    private BigDecimal drivingBehaviorScore;
    private BigDecimal damageHistoryScore;
    private BigDecimal paymentReliabilityScore;
    private BigDecimal cancellationScore;

    // Composite
    private BigDecimal overallScore;
    private String trustGrade;          // EXCELLENT, GOOD, FAIR, POOR

    // Raw counters
    private Integer totalBookings;
    private Integer completedBookings;
    private Integer cancelledBookings;
    private Integer lateReturns;
    private Integer damageIncidents;
    private Integer paymentFailures;
    private Integer paymentSuccess;
    private BigDecimal avgReviewRating;

    private LocalDateTime calculatedAt;
    private LocalDateTime updatedAt;

    // Joined fields for display
    private String userName;
    private String userEmail;
}