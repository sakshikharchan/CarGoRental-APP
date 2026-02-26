package com.example.demo.DTOs;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardStats {
    private Long totalBookings;
    private Long pendingBookings;
    private Long confirmedBookings;
    private Long activeBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private Long totalCars;
    private Long availableCars;
    private Long totalCustomers;
    private Long totalVendors;
    private Long approvedVendors;
    private Long totalDrivers;
    private Long availableDrivers;
    private Long pendingVendorApprovals;
    private Long totalReviews;
    private Double avgRating;
    private Long totalPayments;
    private Long pendingPayments;
    private Long todayBookings;
    private Long todayRevenue;
}