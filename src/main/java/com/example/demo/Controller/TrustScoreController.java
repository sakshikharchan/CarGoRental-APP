package com.example.demo.Controller;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.Model.TrustScore;
import com.example.demo.Model.TrustScoreEvent;
import com.example.demo.Service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Trust Score API
 *
 * GET  /api/trust-score/me               → customer: own score
 * GET  /api/trust-score/me/history       → customer: own event history
 * GET  /api/trust-score/user/{userId}    → admin/vendor: view any user's score
 * POST /api/trust-score/user/{userId}/recalculate → admin: force recalc
 * GET  /api/trust-score/all              → admin: all scores (leaderboard)
 */
@RestController
@RequestMapping("/api/trust-score")
@RequiredArgsConstructor
public class TrustScoreController {

    private final TrustScoreService trustScoreService;

    // ── Customer: view own score ──────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<ApiResponse<TrustScore>> getMyScore(Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        TrustScore score = trustScoreService.getOrCreate(userId);
        return ResponseEntity.ok(ApiResponse.success(score));
    }

    @GetMapping("/me/history")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<ApiResponse<List<TrustScoreEvent>>> getMyHistory(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.ok(ApiResponse.success(
                trustScoreService.getHistory(userId, page, size)));
    }

    // ── Vendor: view renter's score before approving ──────────────────────────

    /**
     * Vendor calls this with the renter's userId when reviewing a booking request.
     * Returns score + grade so vendor can decide whether to approve.
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<TrustScore>> getUserScore(@PathVariable Long userId) {
        try {
            TrustScore score = trustScoreService.getOrCreate(userId);
            return ResponseEntity.ok(ApiResponse.success(score));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Admin: force recalculate ──────────────────────────────────────────────

    @PostMapping("/user/{userId}/recalculate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<TrustScore>> recalculate(@PathVariable Long userId) {
        try {
            TrustScore score = trustScoreService.recalculate(userId);
            return ResponseEntity.ok(ApiResponse.success("Score recalculated", score));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Admin: all scores ─────────────────────────────────────────────────────

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<TrustScore>>> getAllScores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                trustScoreService.getAllScores(page, size)));
    }

    // ── Admin: manually record an event (e.g. damage confirmed) ──────────────

    @PostMapping("/user/{userId}/event")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<TrustScore>> recordEvent(
            @PathVariable Long userId,
            @RequestParam String eventType,
            @RequestParam(required = false) String detail,
            @RequestParam(required = false) Long bookingId) {
        try {
            TrustScore updated = trustScoreService.recordEvent(userId, eventType, detail, bookingId);
            return ResponseEntity.ok(ApiResponse.success("Event recorded", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}