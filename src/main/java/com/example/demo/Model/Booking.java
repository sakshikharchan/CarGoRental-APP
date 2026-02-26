package com.example.demo.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class Booking {
    private Long id;
    private String bookingNumber;
    private Long customerId;
    private Long carId;
    private Long driverId;
    private Long vendorId;
    private String pickupLocation;
    private String dropoffLocation;
    private LocalDate pickupDate;
    private LocalDate returnDate;
    // ✅ FIX: Added - this field is referenced in TrustScoreRepository and was missing from original model
    private LocalDate actualReturnDate;
    private Integer totalDays;
    private BigDecimal dailyRate;
    private BigDecimal totalAmount;
    private Boolean withDriver;
    private BigDecimal driverCharge;
    private BigDecimal extraCharges;
    private BigDecimal discountAmount;
    private String couponCode;
    private BigDecimal securityDeposit;
    private Boolean depositRefunded;
    private String status;
    private String cancellationReason;
    private String cancelledBy;
    private String notes;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // joined fields (not stored in bookings table, populated by joins)
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String carBrand;
    private String carModel;
    private String carRegistrationNo;
    private String driverName;
    private String vendorCompanyName;
}
