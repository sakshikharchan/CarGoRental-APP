package com.example.demo.Service;

import com.example.demo.DTOs.LoginRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.example.demo.DTOs.LoginResponse;
import com.example.demo.DTOs.RegisterRequest;
import com.example.demo.Model.RefreshToken;
import com.example.demo.Model.Role;
import com.example.demo.Model.User;
import com.example.demo.Repository.RefreshTokenRepository;
import com.example.demo.Repository.RoleRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.util.JwtUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JdbcTemplate jdbcTemplate;
    private final EmailService emailService; 

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        
        

        // Determine role: only ROLE_CUSTOMER and ROLE_VENDOR allowed via self-registration
        final String roleName;
        if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
            String requested = request.getRoleName().toUpperCase();
            if (!requested.startsWith("ROLE_")) requested = "ROLE_" + requested;
            // ROLE_ADMIN and ROLE_DRIVER cannot be self-assigned
            roleName = ("ROLE_VENDOR".equals(requested)) ? requested : "ROLE_CUSTOMER";
        } else {
            roleName = "ROLE_CUSTOMER";
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .isActive(true)
                .roleId(role.getId())
                .build();

        long id = userRepository.save(user);
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User creation failed"));
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        String accessToken = jwtUtil.generateToken(user.getEmail(), role.getName(), user.getId());

        // Rotate refresh token
        refreshTokenRepository.deleteByUserId(user.getId());
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(role.getName())
                .build();
    }
    public void sendForgotPasswordEmail(String email) {
        // Check user exists
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Silently return - don't reveal if email is registered
            return;
        }
        User user = userOpt.get();

        // Delete any existing reset token for this user
        jdbcTemplate.update(
            "DELETE FROM password_reset_tokens WHERE user_id = ?",
            user.getId()
        );

        // Create new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);

        jdbcTemplate.update(
            "INSERT INTO password_reset_tokens (user_id, token, expiry_date) VALUES (?, ?, ?)",
            user.getId(), token, expiry
        );

        // Send email
        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Invalid reset token");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT user_id, expiry_date FROM password_reset_tokens WHERE token = ?",
            token
        );

        if (rows.isEmpty()) {
            throw new RuntimeException("Invalid or expired reset link. Please request a new one.");
        }

        Map<String, Object> row = rows.get(0);
        LocalDateTime expiry = ((java.sql.Timestamp) row.get("expiry_date")).toLocalDateTime();

        if (LocalDateTime.now().isAfter(expiry)) {
            jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE token = ?", token);
            throw new RuntimeException("This reset link has expired. Please request a new one.");
        }

        Long userId = (Long) row.get("user_id");

        // Update password
        jdbcTemplate.update(
            "UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?",
            passwordEncoder.encode(newPassword), userId
        );

        // Delete used token
        jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE token = ?", token);

        // Invalidate all refresh tokens for security
        refreshTokenRepository.deleteByUserId(userId);
    }
}