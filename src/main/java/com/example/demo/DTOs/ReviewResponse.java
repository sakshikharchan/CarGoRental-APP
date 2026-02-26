package com.example.demo.DTOs;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    private String customerName;
    private String carName;
}