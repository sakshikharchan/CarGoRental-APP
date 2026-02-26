package com.example.demo.Model;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
    private Long id;
    private Long userId;
    private String action;
    private String entityType;
    private Long entityId;
    private String oldValue;
    private String newValue;
    private String details;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    
    // Joined
    private String userFullName;
    private String userEmail;
}