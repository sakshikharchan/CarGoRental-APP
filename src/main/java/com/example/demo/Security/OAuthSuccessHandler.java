package com.example.demo.Security;

import com.example.demo.DTOs.LoginResponse;
import com.example.demo.Service.OAuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuthService oAuthService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId(); // "google" or "github"

        // Pass raw attributes map — avoids any OAuth2User classpath issues in OAuthService
        Map<String, Object> attributes = oAuth2User.getAttributes();

        try {
            LoginResponse loginResponse = oAuthService.processOAuthUser(attributes, provider);

            // Redirect to frontend /oauth2/callback with tokens in query params
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl + "/oauth2/callback")
                    .queryParam("accessToken",  loginResponse.getAccessToken())
                    .queryParam("refreshToken", loginResponse.getRefreshToken())
                    .queryParam("role",         loginResponse.getRole())
                    .queryParam("userId",       loginResponse.getUserId())
                    .queryParam("fullName",     encode(loginResponse.getFullName()))
                    .queryParam("email",        loginResponse.getEmail())
                    .build(true)   // true = values are already encoded
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            // On failure → send to frontend register page with error message
            String errorUrl = frontendUrl + "/register?error=" + encode(e.getMessage());
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    private String encode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}