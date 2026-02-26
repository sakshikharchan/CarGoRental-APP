package com.example.demo.Service;

import com.example.demo.DTOs.CouponValidateResponse;
import com.example.demo.Model.Coupon;
import com.example.demo.Repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public Coupon createCoupon(Coupon coupon) {
        if (couponRepository.findByCode(coupon.getCode()).isPresent()) {
            throw new RuntimeException("Coupon code already exists");
        }
        long id = couponRepository.save(coupon);
        return couponRepository.findByCode(coupon.getCode())
                .orElseThrow(() -> new RuntimeException("Coupon creation failed"));
    }

    public CouponValidateResponse validateCoupon(String code, BigDecimal bookingAmount) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElse(null);

        if (coupon == null) {
            return CouponValidateResponse.builder().valid(false).message("Invalid coupon code").build();
        }
        if (!coupon.isActive()) {
            return CouponValidateResponse.builder().valid(false).message("Coupon is no longer active").build();
        }
        LocalDate today = LocalDate.now();
        if (coupon.getValidFrom() != null && today.isBefore(coupon.getValidFrom())) {
            return CouponValidateResponse.builder().valid(false).message("Coupon is not yet valid").build();
        }
        if (coupon.getValidUntil() != null && today.isAfter(coupon.getValidUntil())) {
            return CouponValidateResponse.builder().valid(false).message("Coupon has expired").build();
        }
        if (coupon.getMinBookingAmount() != null
                && bookingAmount.compareTo(coupon.getMinBookingAmount()) < 0) {
            return CouponValidateResponse.builder()
                    .valid(false)
                    .message("Minimum booking amount is ₹" + coupon.getMinBookingAmount())
                    .build();
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return CouponValidateResponse.builder().valid(false).message("Coupon usage limit reached").build();
        }

        BigDecimal discountAmount;
        if ("PERCENTAGE".equals(coupon.getDiscountType())) {
            discountAmount = bookingAmount.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
            if (coupon.getMaxDiscount() != null && discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
                discountAmount = coupon.getMaxDiscount();
            }
        } else {
            discountAmount = coupon.getDiscountValue();
        }

        BigDecimal finalAmount = bookingAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        return CouponValidateResponse.builder()
                .valid(true)
                .message("Coupon applied successfully")
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }

    public void toggleCoupon(Long id, boolean active) {
        couponRepository.toggleActive(id, active);
    }
}