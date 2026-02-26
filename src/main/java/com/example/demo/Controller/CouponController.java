package com.example.demo.Controller;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.DTOs.CouponValidateResponse;
import com.example.demo.Model.Coupon;
import com.example.demo.Service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Coupon>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(couponService.getAllCoupons()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Coupon>> create(@RequestBody Coupon coupon) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Coupon created", couponService.createCoupon(coupon)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CouponValidateResponse>> validate(
            @RequestParam String code,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success(couponService.validateCoupon(code, amount)));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggle(
            @PathVariable Long id,
            @RequestParam boolean active) {
        couponService.toggleCoupon(id, active);
        return ResponseEntity.ok(ApiResponse.success("Coupon updated", null));
    }
}