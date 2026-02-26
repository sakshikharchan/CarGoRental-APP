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

public class User {
    private Long id;
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private String address;
    private String profileImage;
    private boolean isActive;
    private Long roleId;
    private String roleName; // joined from roles
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}