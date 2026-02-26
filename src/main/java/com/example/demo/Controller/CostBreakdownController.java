package com.example.demo.Controller;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.DTOs.CostBreakdown;
import com.example.demo.DTOs.CostPreviewRequest;
import com.example.demo.Service.CostBreakdownService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Cost Breakdown API
 *
 * GET  /api/bookings/cost-preview             → full transparent breakdown before booking
 * GET  /api/cost-config/taxes                 → list all state taxes
 * PUT  /api/cost-config/taxes/{stateCode}     → admin: update state tax
 * GET  /api/cost-config/insurance             → list insurance plans
 * POST /api/cost-config/toll-routes           → admin: add/update toll route
 */
@RestController
@RequiredArgsConstructor
public class CostBreakdownController {

    private final CostBreakdownService costBreakdownService;

    // ── Cost Preview (main feature) ───────────────────────────────────────────

    /**
     * Called by the frontend BEFORE the user clicks "Confirm Booking".
     * Returns a full transparent breakdown with every rupee explained.
     *
     * Supports both GET (query params for simple calls) and POST (body for full params).
     */
    @GetMapping("/api/bookings/cost-preview")
    public ResponseEntity<ApiResponse<CostBreakdown>> getCostPreviewGet(
            @RequestParam Long carId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
            @RequestParam(required = false, defaultValue = "false") Boolean withDriver,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) String couponCode,
            @RequestParam(required = false) Long insurancePlanId,
            @RequestParam(required = false, defaultValue = "MH") String pickupStateCode,
            @RequestParam(required = false) String pickupCity,
            @RequestParam(required = false) String dropoffCity) {

        CostPreviewRequest req = new CostPreviewRequest(
                carId, pickupDate, returnDate, withDriver, driverId,
                couponCode, insurancePlanId, pickupStateCode, pickupCity, dropoffCity);
        try {
            CostBreakdown breakdown = costBreakdownService.calculatePreview(req);
            return ResponseEntity.ok(ApiResponse.success(breakdown));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/api/bookings/cost-preview")
    public ResponseEntity<ApiResponse<CostBreakdown>> getCostPreviewPost(
            @RequestBody CostPreviewRequest req) {
        try {
            CostBreakdown breakdown = costBreakdownService.calculatePreview(req);
            return ResponseEntity.ok(ApiResponse.success(breakdown));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Admin: State Tax Config ───────────────────────────────────────────────

    @GetMapping("/api/cost-config/taxes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllTaxes() {
        return ResponseEntity.ok(ApiResponse.success(costBreakdownService.getAllStateTaxes()));
    }

    @PutMapping("/api/cost-config/taxes/{stateCode}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateTax(
            @PathVariable String stateCode,
            @RequestParam String stateName,
            @RequestParam BigDecimal taxRate) {
        try {
            costBreakdownService.upsertStateTax(stateCode, stateName, taxRate);
            return ResponseEntity.ok(ApiResponse.success("Tax updated", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Admin: Insurance Plans ────────────────────────────────────────────────

    @GetMapping("/api/cost-config/insurance")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getInsurancePlans(
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(
                costBreakdownService.getInsurancePlans(categoryId)));
    }

    // ── Admin: Toll Routes ────────────────────────────────────────────────────

    @PostMapping("/api/cost-config/toll-routes")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> upsertTollRoute(
            @RequestParam String fromCity,
            @RequestParam String toCity,
            @RequestParam BigDecimal estimatedToll,
            @RequestParam(required = false) BigDecimal distanceKm) {
        try {
            costBreakdownService.upsertTollRoute(fromCity, toCity, estimatedToll, distanceKm);
            return ResponseEntity.ok(ApiResponse.success("Toll route updated", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}