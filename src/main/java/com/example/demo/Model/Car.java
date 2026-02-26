package com.example.demo.Model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Car {
    private Long id;
    private Long vendorId;
    private Long categoryId;

    // "make" used by some frontend code instead of "brand"
    @JsonAlias({"make"})
    private String brand;

    private String model;
    private Integer year;

    // ✅ Accept all common field name variants for registration
    @JsonAlias({"registrationNumber", "registration_no", "regNo", "plateNumber", "plate_number"})
    private String registrationNo;

    private String color;
    private Integer seats;

    @JsonAlias({"fuel_type"})
    private String fuelType;

    private String transmission;

    @JsonAlias({"daily_rate", "price", "pricePerDay", "ratePerDay"})
    private BigDecimal dailyRate;

    @JsonAlias({"image_url", "image", "photo", "photoUrl"})
    private String imageUrl;

    // ✅ KEY FIX: @JsonProperty("isAvailable") forces Jackson to serialize this
    // field as "isAvailable" in ALL responses (GET, POST, PUT).
    //
    // ROOT CAUSE OF THE BUG:
    // Without @JsonProperty, Jackson sees the Lombok-generated getter getAvailable()
    // and serializes the field as "available" in responses, but the frontend
    // CarsPage.jsx sends { isAvailable: true } on save/edit — causing a silent mismatch.
    //
    // This fix ensures the JSON key is always "isAvailable" in both directions:
    //   - GET /api/cars        → returns { ..., "isAvailable": true }
    //   - POST/PUT /api/cars   → accepts { ..., "isAvailable": true }
    //
    // @JsonAlias still lets the backend accept "available" or "is_available"
    // from any legacy callers.
    @JsonProperty("isAvailable")
    @JsonAlias({"available", "is_available"})
    private Boolean isAvailable;

    private String categoryName; // joined field
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}