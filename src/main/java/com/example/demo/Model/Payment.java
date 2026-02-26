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
@JsonIgnoreProperties(ignoreUnknown = true)   // ← add this

public class Payment {
    private Long id;
    private Long bookingId;
    private String transactionId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;

    // Joined fields
    private String bookingNumber;
    private String customerName;
}