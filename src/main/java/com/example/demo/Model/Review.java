package com.example.demo.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)   // ← add this

public class Review {
    private Long id;
    private Long bookingId;
    private Long customerId;
    private Long carId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    // Joined fields
    private String customerName;
    private String carBrand;
    private String carModel;
    private String bookingNumber;
}