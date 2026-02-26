package com.example.demo.Service;

import com.example.demo.DTOs.LoginRequest;
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
}