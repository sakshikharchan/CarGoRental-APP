package com.example.demo.Controller;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.Model.DamageReport;
import com.example.demo.Service.DamageReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Damage Report API
 *
 * POST /api/damage-reports                       → vendor/admin: file damage report
 * GET  /api/damage-reports                       → admin: all reports
 * GET  /api/damage-reports/booking/{bookingId}   → vendor/admin: by booking
 * PATCH /api/damage-reports/{id}/status          → admin: resolve/dispute
 */
@RestController
@RequestMapping("/api/damage-reports")
@RequiredArgsConstructor
public class DamageReportController {

    private final DamageReportService damageReportService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<DamageReport>> reportDamage(
            @RequestBody DamageReport report,
            Authentication auth) {
        try {
            Long reportedByUserId = (Long) auth.getCredentials();
            DamageReport saved = damageReportService.reportDamage(reportedByUserId, report);
            return ResponseEntity.ok(ApiResponse.success("Damage report filed", saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<DamageReport>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                damageReportService.getAll(page, size)));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<DamageReport>>> getByBooking(
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(
                damageReportService.getByBooking(bookingId)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<DamageReport>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            DamageReport updated = damageReportService.resolveReport(id, status);
            return ResponseEntity.ok(ApiResponse.success("Status updated to " + status, updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}