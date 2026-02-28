package com.example.demo.Service;

import com.example.demo.DTOs.LoginResponse;
import com.example.demo.Model.RefreshToken;
import com.example.demo.Model.Role;
import com.example.demo.Model.User;
import com.example.demo.Repository.RefreshTokenRepository;
import com.example.demo.Repository.RoleRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    /**
     * Called after successful OAuth2 login.
     * Accepts raw attributes map instead of OAuth2User to avoid classpath issues.
     * Finds or creates user, then returns JWT tokens identical to normal login.
     *
     * @param attributes  the OAuth2User.getAttributes() map (String → Object)
     * @param provider    "google" or "github"
     */
    public LoginResponse processOAuthUser(Map<String, Object> attributes, String provider) {

        String email    = extractEmail(attributes, provider);
        String fullName = extractName(attributes, provider);

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Could not get email from " + provider + " account. "
                    + "Please make sure your email is public on " + provider + ".");
        }

        // ── Find existing user OR auto-register as ROLE_CUSTOMER ─────────────
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("ROLE_CUSTOMER not found in DB"));

            User newUser = User.builder()
                    .fullName(fullName != null && !fullName.isBlank()
                            ? fullName
                            : email.split("@")[0])
                    .email(email)
                    .password("")        // No password — OAuth user
                    .phone("")
                    .isActive(true)
                    .roleId(customerRole.getId())
                    .build();

            long id = userRepository.save(newUser);
            return userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("OAuth user creation failed"));
        });

        if (!user.isActive()) {
            throw new RuntimeException("Your account has been deactivated. Please contact support.");
        }

        // ── Issue JWT — identical to AuthService.login() ─────────────────────
        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found for user"));

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

    // ── Private helpers ───────────────────────────────────────────────────────

    private String extractEmail(Map<String, Object> attrs, String provider) {
        if ("github".equalsIgnoreCase(provider)) {
            Object email = attrs.get("email");
            if (email != null && !email.toString().isBlank()) return email.toString();
            // GitHub hides email if set to private — use login as fallback
            Object login = attrs.get("login");
            return login != null ? login.toString() + "@github.noemail" : null;
        }
        // Google always provides email
        Object email = attrs.get("email");
        return email != null ? email.toString() : null;
    }

    private String extractName(Map<String, Object> attrs, String provider) {
        if ("github".equalsIgnoreCase(provider)) {
            Object name = attrs.get("name");
            if (name != null && !name.toString().isBlank()) return name.toString();
            Object login = attrs.get("login");
            return login != null ? login.toString() : null;
        }
        Object name = attrs.get("name");
        return name != null ? name.toString() : null;
    }
}