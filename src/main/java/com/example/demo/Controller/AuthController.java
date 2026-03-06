package com.example.demo.Controller;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.example.demo.DTOs.ApiResponse;
import com.example.demo.DTOs.LoginRequest;
import com.example.demo.DTOs.LoginResponse;
import com.example.demo.DTOs.RegisterRequest;
import com.example.demo.Model.User;
import com.example.demo.Service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);
            user.setPassword(null); // Never expose hashed password
            return ResponseEntity.ok(ApiResponse.success("Registration successful", user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            authService.sendForgotPasswordEmail(body.get("email"));
            // Always return success (don't reveal if email exists)
            return ResponseEntity.ok(ApiResponse.success("If this email is registered, a reset link has been sent.", null));
        } catch (Exception e) {
            // Still return success to prevent email enumeration attacks
            return ResponseEntity.ok(ApiResponse.success("If this email is registered, a reset link has been sent.", null));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody Map<String, String> body) {
        try {
            authService.resetPassword(body.get("token"), body.get("newPassword"));
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}