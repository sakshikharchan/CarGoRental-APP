package com.example.demo.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class Vendor {
    private Long id;
    private Long userId;
    private String companyName;
    private String licenseNumber;
    private String gstNumber;
    private BigDecimal rating;
    private boolean isApproved;
    private LocalDateTime createdAt;

    // Joined fields
    private String userFullName;
    private String userEmail;
    private String userPhone;
}