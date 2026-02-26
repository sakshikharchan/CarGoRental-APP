package com.example.demo.Service;

import com.example.demo.DTOs.CostBreakdown;
import com.example.demo.DTOs.CostBreakdown.LineItem;
import com.example.demo.DTOs.CostPreviewRequest;
import com.example.demo.DTOs.CouponValidateResponse;
import com.example.demo.Model.Car;
import com.example.demo.Repository.CarRepository;
import com.example.demo.Repository.CostBreakdownRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Transparent Cost Breakdown Engine
 *
 * Calculates EVERY cost component before the user confirms booking:
 *   1. Base rent
 *   2. Driver charge (optional)
 *   3. Insurance premium
 *   4. State GST
 *   5. Estimated tolls (city-pair lookup)
 *   6. Security deposit (refundable)
 *   7. Coupon discount
 *   8. Fuel policy explanation
 *   9. Late return penalty rules
 */
@Service
@RequiredArgsConstructor
public class CostBreakdownService {

    private final CarRepository carRepository;
    private final CostBreakdownRepository costRepo;
    private final CouponService couponService;

    private static final BigDecimal DRIVER_DAILY_RATE   = BigDecimal.valueOf(500);
    private static final BigDecimal SECURITY_DEPOSIT_MULTIPLIER = BigDecimal.valueOf(3);

    public CostBreakdown calculatePreview(CostPreviewRequest req) {

        // ── 1. Fetch car ──────────────────────────────────────────────────────
        Car car = carRepository.findById(req.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found: " + req.getCarId()));

        long totalDays = ChronoUnit.DAYS.between(req.getPickupDate(), req.getReturnDate());
        if (totalDays <= 0) throw new RuntimeException("Return date must be after pickup date");

        List<LineItem> lineItems = new ArrayList<>();

        // ── 2. Base Rent ──────────────────────────────────────────────────────
        BigDecimal dailyRate = car.getDailyRate();
        BigDecimal baseRent  = dailyRate.multiply(BigDecimal.valueOf(totalDays));
        lineItems.add(LineItem.builder()
                .label("Base Rent (" + totalDays + " day" + (totalDays > 1 ? "s" : "") +
                       " × ₹" + dailyRate + ")")
                .amount(baseRent)
                .type("BASE")
                .note("Daily rate × number of rental days")
                .isRefundable(false).isEstimate(false)
                .build());

        // ── 3. Driver Charge ──────────────────────────────────────────────────
        BigDecimal driverTotal = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(req.getWithDriver())) {
            driverTotal = DRIVER_DAILY_RATE.multiply(BigDecimal.valueOf(totalDays));
            lineItems.add(LineItem.builder()
                    .label("Driver Charge (" + totalDays + " day" + (totalDays > 1 ? "s" : "") +
                           " × ₹" + DRIVER_DAILY_RATE + ")")
                    .amount(driverTotal)
                    .type("ADDON")
                    .note("Includes driver's daily allowance. Tips are optional.")
                    .isRefundable(false).isEstimate(false)
                    .build());
        }

        // ── 4. Insurance ──────────────────────────────────────────────────────
        BigDecimal insuranceTotal = BigDecimal.ZERO;
        String insurancePlanName  = "Basic Protection";
        String insuranceCoverage  = "₹5,00,000 coverage";
        BigDecimal insuranceDailyPremium = BigDecimal.ZERO;

        Map<String, Object> insurancePlan = null;
        if (req.getInsurancePlanId() != null) {
            insurancePlan = costRepo.findInsurancePlanById(req.getInsurancePlanId());
        }
        if (insurancePlan == null) {
            insurancePlan = costRepo.findMandatoryInsurancePlan(car.getCategoryId());
        }
        if (insurancePlan != null) {
            insuranceDailyPremium = new BigDecimal(insurancePlan.get("daily_premium").toString());
            insuranceTotal        = insuranceDailyPremium.multiply(BigDecimal.valueOf(totalDays));
            insurancePlanName     = insurancePlan.get("plan_name").toString();
            Object cov            = insurancePlan.get("coverage_amount");
            if (cov != null) {
                long coverageVal = new BigDecimal(cov.toString()).longValue();
                insuranceCoverage = "₹" + String.format("%,d", coverageVal) + " coverage";
            }
        }
        if (insuranceTotal.compareTo(BigDecimal.ZERO) > 0) {
            lineItems.add(LineItem.builder()
                    .label("Insurance – " + insurancePlanName +
                           " (" + totalDays + " day" + (totalDays > 1 ? "s" : "") +
                           " × ₹" + insuranceDailyPremium + ")")
                    .amount(insuranceTotal)
                    .type("ADDON")
                    .note(insuranceCoverage + ". Mandatory third-party cover as per Motor Vehicles Act.")
                    .isRefundable(false).isEstimate(false)
                    .build());
        }

        // ── 5. Toll Estimate ─────────────────────────────────────────────────
        BigDecimal tollEstimate = BigDecimal.ZERO;
        String tollNote = "No toll route found. Actual tolls will be billed at return.";
        if (req.getPickupCity() != null && req.getDropoffCity() != null
                && !req.getPickupCity().isBlank() && !req.getDropoffCity().isBlank()) {
            tollEstimate = costRepo.findTollEstimate(req.getPickupCity(), req.getDropoffCity());
            if (tollEstimate.compareTo(BigDecimal.ZERO) > 0) {
                tollNote = "Estimated one-way toll for " + req.getPickupCity() +
                           " → " + req.getDropoffCity() + ". Actual toll may vary.";
            }
        }
        lineItems.add(LineItem.builder()
                .label("Estimated Tolls")
                .amount(tollEstimate)
                .type(tollEstimate.compareTo(BigDecimal.ZERO) > 0 ? "ADDON" : "INFO")
                .note(tollNote)
                .isRefundable(false).isEstimate(true)
                .build());

        // ── 6. Subtotal before tax ────────────────────────────────────────────
        BigDecimal subtotal = baseRent
                .add(driverTotal)
                .add(insuranceTotal)
                .add(tollEstimate);

        // ── 7. State Tax (GST) ────────────────────────────────────────────────
        BigDecimal taxRate   = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        String stateName     = "Unknown";
        String stateCode     = req.getPickupStateCode();

        if (stateCode != null && !stateCode.isBlank()) {
            Map<String, Object> taxRow = costRepo.findStateTax(stateCode);
            if (taxRow != null) {
                taxRate  = new BigDecimal(taxRow.get("tax_rate").toString());
                stateName = taxRow.get("state_name").toString();
                taxAmount = subtotal.multiply(taxRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                lineItems.add(LineItem.builder()
                        .label("GST (" + stateName + " – " + taxRate + "%)")
                        .amount(taxAmount)
                        .type("TAX")
                        .note("Goods & Services Tax as applicable in " + stateName +
                              ". Calculated on base rent + add-ons.")
                        .isRefundable(false).isEstimate(false)
                        .build());
            }
        }

        // ── 8. Coupon Discount ────────────────────────────────────────────────
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCoupon = null;
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            CouponValidateResponse couponResult = couponService.validateCoupon(
                    req.getCouponCode(), subtotal);
            if (couponResult.isValid()) {
                discountAmount = couponResult.getDiscountAmount();
                appliedCoupon  = req.getCouponCode().toUpperCase();
                lineItems.add(LineItem.builder()
                        .label("Coupon Discount – " + appliedCoupon)
                        .amount(discountAmount.negate())
                        .type("DISCOUNT")
                        .note(couponResult.getMessage())
                        .isRefundable(false).isEstimate(false)
                        .build());
            }
        }

        // ── 9. Grand Total ────────────────────────────────────────────────────
        BigDecimal grandTotal = subtotal.add(taxAmount).subtract(discountAmount)
                .max(BigDecimal.ZERO);

        // ── 10. Security Deposit (refundable, shown separately) ───────────────
        BigDecimal securityDeposit = dailyRate.multiply(SECURITY_DEPOSIT_MULTIPLIER)
                .setScale(2, RoundingMode.HALF_UP);
        lineItems.add(LineItem.builder()
                .label("Security Deposit (Refundable)")
                .amount(securityDeposit)
                .type("DEPOSIT")
                .note("Fully refunded within 7 working days after return, " +
                      "provided no damage or traffic violations.")
                .isRefundable(true).isEstimate(false)
                .build());

        // ── 11. Fuel Policy ───────────────────────────────────────────────────
        String fuelPolicy = "FULL_TO_FULL"; // default if DB column not yet set
        String fuelNote   = buildFuelPolicyNote(fuelPolicy);

        // ── 12. Late Return Rules ─────────────────────────────────────────────
        Map<String, Object> penalty = costRepo.findLatePenalty(car.getVendorId());
        int    gracePeriodHours = 1;
        BigDecimal penaltyPerHour = BigDecimal.valueOf(150);
        String lateReturnNote;
        if (penalty != null) {
            gracePeriodHours = Integer.parseInt(penalty.get("grace_hours").toString());
            penaltyPerHour   = new BigDecimal(penalty.get("penalty_per_hour").toString());
            int maxDays      = Integer.parseInt(penalty.get("max_penalty_days").toString());
            lateReturnNote   = "After " + gracePeriodHours + " hour grace period, " +
                               "₹" + penaltyPerHour + "/hour is charged. " +
                               "Capped at " + maxDays + " extra day(s) charge.";
        } else {
            lateReturnNote = "₹150/hour after 1 hour grace period. Capped at 2 extra days.";
        }

        return CostBreakdown.builder()
                // Core
                .dailyRate(dailyRate)
                .totalDays((int) totalDays)
                .baseRent(baseRent)
                // Driver
                .withDriver(Boolean.TRUE.equals(req.getWithDriver()))
                .driverChargePerDay(DRIVER_DAILY_RATE)
                .driverChargeTotal(driverTotal)
                // Insurance
                .insurancePlanName(insurancePlanName)
                .insuranceDailyPremium(insuranceDailyPremium)
                .insuranceTotal(insuranceTotal)
                .insuranceCoverage(insuranceCoverage)
                // Tax
                .stateName(stateName)
                .stateCode(stateCode)
                .taxRate(taxRate)
                .taxAmount(taxAmount)
                // Toll
                .estimatedToll(tollEstimate)
                .tollNote(tollNote)
                // Fuel
                .fuelPolicy(fuelPolicy)
                .fuelPolicyNote(fuelNote)
                // Deposit
                .securityDeposit(securityDeposit)
                .depositNote("Refunded within 7 working days. Subject to damage inspection.")
                // Late
                .gracePeriodHours(gracePeriodHours)
                .penaltyPerHour(penaltyPerHour)
                .lateReturnNote(lateReturnNote)
                // Discount
                .couponCode(appliedCoupon)
                .discountAmount(discountAmount)
                // Totals
                .subtotal(subtotal)
                .totalTax(taxAmount)
                .totalDiscount(discountAmount)
                .grandTotal(grandTotal)
                .refundableDeposit(securityDeposit)
                // All items
                .lineItems(lineItems)
                .build();
    }

    // ── Admin helpers ─────────────────────────────────────────────────────────

    public List<Map<String, Object>> getAllStateTaxes() {
        return costRepo.findAllStateTaxes();
    }

    public List<Map<String, Object>> getInsurancePlans(Long categoryId) {
        return costRepo.findInsurancePlans(categoryId);
    }

    public void upsertStateTax(String stateCode, String stateName, BigDecimal taxRate) {
        costRepo.upsertStateTax(stateCode, stateName, taxRate);
    }

    public void upsertTollRoute(String from, String to, BigDecimal toll, BigDecimal km) {
        costRepo.upsertTollRoute(from, to, toll, km);
        // Also seed reverse
        costRepo.upsertTollRoute(to, from, toll, km);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildFuelPolicyNote(String policy) {
        return switch (policy) {
            case "FULL_TO_FULL"   ->
                "Return with a full tank. If returned empty, refuelling cost + ₹200 service charge applies.";
            case "SAME_TO_SAME"   ->
                "Return with the same fuel level as picked up. Difference is charged at ₹120/litre.";
            case "PRE_PURCHASED"  ->
                "Fuel is pre-paid at a fixed rate. Unused fuel is non-refundable.";
            default ->
                "Fuel policy as agreed with vendor. Clarify at pickup.";
        };
    }
}