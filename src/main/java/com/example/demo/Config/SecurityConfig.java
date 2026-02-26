package com.example.demo.Config;

import com.example.demo.Security.JwtAuthFilter;
import com.example.demo.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtUtil);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // ══════════════════════════════════════════════
                // 1. FULLY PUBLIC — no token needed
                // ══════════════════════════════════════════════
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Public GET endpoints
                .requestMatchers(HttpMethod.GET,  "/api/cars").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/cars/available").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/cars/search").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/cars/{id}").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/categories").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/reviews/car/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/bookings/track/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/drivers/available").permitAll()

                // Cost preview — guests can simulate
                .requestMatchers(HttpMethod.GET,  "/api/bookings/cost-preview").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/bookings/cost-preview").permitAll()

                // Cost config — publicly readable
                .requestMatchers(HttpMethod.GET,  "/api/cost-config/insurance").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/cost-config/taxes").permitAll()

                // Coupon validate — public
                .requestMatchers(HttpMethod.POST, "/api/coupons/validate").permitAll()

                // Vendor registration — public (anyone can apply)
                .requestMatchers(HttpMethod.POST, "/api/vendors").permitAll()

                // ══════════════════════════════════════════════
                // 2. ADMIN ONLY
                // ══════════════════════════════════════════════
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/dashboard/**").hasAuthority("ROLE_ADMIN")

                // Coupon management — admin only (except validate above)
                .requestMatchers(HttpMethod.GET,   "/api/coupons").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST,  "/api/coupons").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/coupons/**").hasAuthority("ROLE_ADMIN")

                // Trust score — admin special actions
                .requestMatchers(HttpMethod.GET,  "/api/trust-score/all").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/trust-score/user/*/recalculate").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/trust-score/user/*/event").hasAuthority("ROLE_ADMIN")

                // Cost config — admin write
                .requestMatchers(HttpMethod.PUT,  "/api/cost-config/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/cost-config/**").hasAuthority("ROLE_ADMIN")

                // Damage reports — admin only for all/status
                .requestMatchers(HttpMethod.GET,   "/api/damage-reports").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDOR")
                .requestMatchers(HttpMethod.PATCH, "/api/damage-reports/*/status").hasAuthority("ROLE_ADMIN")

                // Reviews — admin can see all
                .requestMatchers(HttpMethod.GET, "/api/reviews").hasAuthority("ROLE_ADMIN")

                // Payments — admin only for all/refund
                .requestMatchers(HttpMethod.GET,  "/api/payments").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/payments/*/refund").hasAuthority("ROLE_ADMIN")

                // ══════════════════════════════════════════════
                // 3. ADMIN + VENDOR
                // ══════════════════════════════════════════════

                // Vendor management (GET list/pending/byId — admin+vendor)
                .requestMatchers(HttpMethod.GET,   "/api/vendors").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDOR")
                .requestMatchers(HttpMethod.GET,   "/api/vendors/pending").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDOR")
                .requestMatchers(HttpMethod.GET,   "/api/vendors/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDOR")
                .requestMatchers(HttpMethod.PUT,   "/api/vendors/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDOR")
                .requestMatchers(HttpMethod.PATCH, "/api/vendors/**").hasAuthority("ROLE_ADMIN")

                // Cars — write operations (must be BEFORE generic GET /api/cars/**)
                .requestMatchers(HttpMethod.GET,    "/api/cars/vendor/**").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/cars").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/cars/**").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/cars/**").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/cars/**").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")

                // Drivers — specific paths BEFORE generic patterns
                .requestMatchers(HttpMethod.GET,   "/api/drivers").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDOR")
                .requestMatchers(HttpMethod.GET,   "/api/drivers/vendor/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDOR")
                .requestMatchers(HttpMethod.POST,  "/api/drivers").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/drivers/*/availability").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN", "ROLE_DRIVER")
                .requestMatchers(HttpMethod.GET,   "/api/drivers/**").authenticated()

                // Damage reports — vendor can file + view by booking
                .requestMatchers(HttpMethod.POST, "/api/damage-reports").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/damage-reports/booking/**").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")

                // Trust score — vendor views renter score
                .requestMatchers(HttpMethod.GET,  "/api/trust-score/user/**").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")

                // Vendor portal
                .requestMatchers("/api/vendor/**").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")

                // ══════════════════════════════════════════════
                // 4. CUSTOMER SPECIFIC (ADMIN can also access)
                // ══════════════════════════════════════════════
                .requestMatchers("/api/customer/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers("/api/trust-score/me/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers("/api/trust-score/me").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")

                // ══════════════════════════════════════════════
                // 5. DRIVER SPECIFIC (ADMIN can also access)
                // ══════════════════════════════════════════════
                .requestMatchers("/api/driver/**").hasAnyAuthority("ROLE_DRIVER", "ROLE_ADMIN")

                // ══════════════════════════════════════════════
                // 6. ANY AUTHENTICATED USER
                // ══════════════════════════════════════════════
                .requestMatchers("/api/notifications/**").authenticated()

                // Bookings — specific paths BEFORE generic patterns
                .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/status").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDOR")
                .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/cancel").authenticated()
                .requestMatchers(HttpMethod.POST,  "/api/bookings").authenticated()
                .requestMatchers(HttpMethod.GET,   "/api/bookings/my").authenticated()
                .requestMatchers(HttpMethod.GET,   "/api/bookings/**").authenticated()

                // Payments — customer/admin process
                .requestMatchers(HttpMethod.POST, "/api/payments/process/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/payments/booking/**").authenticated()

                // Reviews — customer submits
                .requestMatchers(HttpMethod.POST, "/api/reviews").hasAuthority("ROLE_CUSTOMER")

                // ══════════════════════════════════════════════
                // 7. EVERYTHING ELSE — must be authenticated
                // ══════════════════════════════════════════════
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:4173",
            "http://localhost:5174"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}