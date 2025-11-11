package com.minisocial.service;

import com.minisocial.dto.LoginRequest;
import com.minisocial.dto.LoginResponse;
import com.minisocial.dto.RegisterRequest;
import com.minisocial.dto.RegisterResponse;
import com.minisocial.exception.ResourceConflictException;
import com.minisocial.exception.UnauthorizedException;
import com.minisocial.model.User;
import com.minisocial.repository.UserRepository;
import com.minisocial.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.email())) {
            throw new ResourceConflictException("Email already exists");
        }

        // Hash password with BCrypt
        String passwordHash = passwordEncoder.encode(request.password());

        // Create and save user
        User user = new User(request.email(), passwordHash);
        user = userRepository.save(user);

        return new RegisterResponse(user.getId(), user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return new LoginResponse(token, user.getId(), user.getEmail());
    }
}
