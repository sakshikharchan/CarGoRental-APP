package com.example.demo.Model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrustScoreEvent {

    private Long id;
    private Long userId;

    /**
     * Event types:
     * BOOKING_COMPLETED  – positive
     * BOOKING_CANCELLED  – negative
     * LATE_RETURN        – negative
     * DAMAGE_REPORTED    – negative (severity-weighted)
     * PAYMENT_FAILED     – negative
     * PAYMENT_SUCCESS    – positive
     * REVIEW_RECEIVED    – positive/negative based on rating
     */
    private String eventType;
    private String eventDetail;
    private BigDecimal scoreImpact;   // +ve = improved, -ve = worsened
    private Long bookingId;
    private LocalDateTime createdAt;
}