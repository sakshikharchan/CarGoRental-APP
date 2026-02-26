package com.example.demo.Security;

import com.example.demo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = parseJwt(request);
            if (token != null && jwtUtil.validateToken(token)) {
                String email  = jwtUtil.getEmailFromToken(token);
                String role   = jwtUtil.getRoleFromToken(token);
                Long   userId = jwtUtil.getUserIdFromToken(token);
                  
                logger.debug("JWT Auth: email={}, role={}, userId={}, uri={} {}",
                        email, role, userId, request.getMethod(), request.getRequestURI());

                // Guard: if role or userId is null, this is likely a refresh token — skip auth
                if (role == null || userId == null) {
                    logger.warn("Token missing role or userId claim (possibly a refresh token). Skipping authentication.");
                } else {
                    // Ensure role has ROLE_ prefix
                    String normalizedRole = role;
                    if (!role.startsWith("ROLE_")) {
                        normalizedRole = "ROLE_" + role;
                    }

                    List<SimpleGrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority(normalizedRole));

                    // principal = email, credentials = userId
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(email, userId, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } else if (token != null) {
                logger.warn("Token present but invalid for request: {} {}",
                        request.getMethod(), request.getRequestURI());
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage(), e);
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}