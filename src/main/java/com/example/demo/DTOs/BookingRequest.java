package com.example.demo.DTOs;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BookingRequest {
    private Long carId;
    private Long driverId;
    private String pickupLocation;
    private String dropoffLocation;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pickupDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate returnDate;

    private Boolean withDriver;
    private String notes;
    private String couponCode;
}