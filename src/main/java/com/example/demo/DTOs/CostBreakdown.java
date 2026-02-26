package com.example.demo.DTOs;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Returned by GET /api/bookings/cost-preview  (before booking is created)
 * Shows every rupee the renter will pay — no hidden charges.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CostBreakdown {

    // ── Rental Core ─────────────────────────────────────────────────────────
    private BigDecimal dailyRate;
    private Integer    totalDays;
    private BigDecimal baseRent;           // dailyRate × totalDays

    // ── Optional Add-ons ────────────────────────────────────────────────────
    private Boolean    withDriver;
    private BigDecimal driverChargePerDay;
    private BigDecimal driverChargeTotal;

    // ── Insurance ───────────────────────────────────────────────────────────
    private String     insurancePlanName;
    private BigDecimal insuranceDailyPremium;
    private BigDecimal insuranceTotal;
    private String     insuranceCoverage;  // e.g. "₹10,00,000 coverage"

    // ── Tax ─────────────────────────────────────────────────────────────────
    private String     stateName;
    private String     stateCode;
    private BigDecimal taxRate;            // e.g. 18.00
    private BigDecimal taxAmount;

    // ── Toll ────────────────────────────────────────────────────────────────
    private BigDecimal estimatedToll;
    private String     tollNote;           // "Estimated. Actual tolls billed at return."

    // ── Fuel ────────────────────────────────────────────────────────────────
    private String     fuelPolicy;         // FULL_TO_FULL / SAME_TO_SAME / PRE_PURCHASED
    private String     fuelPolicyNote;

    // ── Security Deposit ────────────────────────────────────────────────────
    private BigDecimal securityDeposit;    // refundable
    private String     depositNote;

    // ── Late Return Rules ───────────────────────────────────────────────────
    private Integer    gracePeriodHours;
    private BigDecimal penaltyPerHour;
    private String     lateReturnNote;

    // ── Discount ────────────────────────────────────────────────────────────
    private String     couponCode;
    private BigDecimal discountAmount;

    // ── Totals ──────────────────────────────────────────────────────────────
    private BigDecimal subtotal;           // baseRent + driver + insurance + toll
    private BigDecimal totalTax;
    private BigDecimal totalDiscount;
    private BigDecimal grandTotal;         // what renter pays now
    private BigDecimal refundableDeposit;  // shown separately

    // ── Line Items (for UI rendering) ───────────────────────────────────────
    private List<LineItem> lineItems;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LineItem {
        private String  label;
        private BigDecimal amount;
        private String  type;       // BASE | ADDON | TAX | DISCOUNT | DEPOSIT | INFO
        private String  note;       // tooltip text for the UI
        private boolean isRefundable;
        private boolean isEstimate;
    }
}