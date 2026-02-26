package com.example.demo.DTOs;

import lombok.*;
import java.time.LocalDate;

/**
 * Sent by frontend to GET /api/bookings/cost-preview
 * before the user actually confirms the booking.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class CostPreviewRequest {
    private Long      carId;
    private LocalDate pickupDate;
    private LocalDate returnDate;
    private Boolean   withDriver;
    private Long      driverId;
    private String    couponCode;
    private Long      insurancePlanId;   // optional – if null, use cheapest mandatory
    private String    pickupStateCode;   // e.g. "MH" — for tax lookup
    private String    pickupCity;        // for toll estimation
    private String    dropoffCity;
}