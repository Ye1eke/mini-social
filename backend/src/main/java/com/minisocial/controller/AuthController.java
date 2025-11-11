package com.minisocial.controller;

import com.minisocial.dto.LoginRequest;
import com.minisocial.dto.LoginResponse;
import com.minisocial.dto.RegisterRequest;
import com.minisocial.dto.RegisterResponse;
import com.minisocial.exception.RateLimitExceededException;
import com.minisocial.service.AuthService;
import com.minisocial.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiter rateLimiter;

    public AuthController(AuthService authService, RateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = extractClientIp(httpRequest);

        // Check rate limit
        if (!rateLimiter.allowRequest(clientIp)) {
            throw new RateLimitExceededException("Too many requests. Please try again later.");
        }

        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = extractClientIp(httpRequest);

        // Check rate limit
        if (!rateLimiter.allowRequest(clientIp)) {
            throw new RateLimitExceededException("Too many requests. Please try again later.");
        }

        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Extracts the client IP address from the HTTP request.
     * Checks X-Forwarded-For header first (for proxied requests), then falls back to remote address.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
