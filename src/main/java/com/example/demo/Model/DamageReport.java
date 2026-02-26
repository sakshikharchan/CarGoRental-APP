package com.example.demo.Model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class DamageReport {

    private Long id;
    private Long carId;
    private Long bookingId;
    private Long reportedBy;

    /** MINOR | MODERATE | SEVERE */
    private String severity;

    private String description;
    private BigDecimal repairCost;
    private Boolean isCustomerFault;
    private String photoUrl;

    /** OPEN | RESOLVED | DISPUTED */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Joined
    private String bookingNumber;
    private String customerName;
    private String carBrand;
    private String carModel;
}