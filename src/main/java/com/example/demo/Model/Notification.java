package com.example.demo.Model;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String type; // BOOKING, PAYMENT, SYSTEM, PROMOTION
    private Long referenceId;
    private String referenceType;
    private boolean isRead;
    private LocalDateTime createdAt;
}