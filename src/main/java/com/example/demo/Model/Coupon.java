package com.example.demo.Model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // ✅ Ignore any unknown fields silently
public class Coupon {
    private Long id;
    private String code;
    private String description;
    private String discountType; // PERCENTAGE, FIXED
    private BigDecimal discountValue;

    @JsonAlias({"minAmount", "min_amount", "minimumAmount"})  // ✅ Accept multiple field names
    private BigDecimal minBookingAmount;

    @JsonAlias({"maxDiscount", "max_discount", "maximumDiscount"})
    private BigDecimal maxDiscount;

    private Integer usageLimit;
    private Integer usedCount;
    private boolean isActive;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private LocalDateTime createdAt;
}