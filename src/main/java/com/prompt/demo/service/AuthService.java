package com.prompt.demo.service;

import com.prompt.demo.dto.request.AuthDTOs;
import com.prompt.demo.dto.response.AuthResponse;
import com.prompt.demo.entity.TokenBlacklist;
import com.prompt.demo.entity.User;
import com.prompt.demo.exception.AppExceptions;
import com.prompt.demo.repository.TokenBlacklistRepository;
import com.prompt.demo.repository.UserRepository;
import com.prompt.demo.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Transactional
    public AuthResponse register(AuthDTOs.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppExceptions.DuplicateResourceException(
                    "Email already registered: " + request.getEmail()
            );
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppExceptions.DuplicateResourceException(
                    "Username already taken: " + request.getUsername()
            );
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        String token = jwtUtil.generateToken(user);
        return buildAuthResponse(user, token);
    }

    public AuthResponse login(AuthDTOs.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();
        String token = jwtUtil.generateToken(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, token);
    }

    @Transactional
    public void logout(String token) {
        if (tokenBlacklistRepository.existsByToken(token)) {
            // Already revoked — treat as a no-op so the endpoint stays idempotent
            log.debug("Logout called with an already-blacklisted token");
            return;
        }

        Instant expiresAt = jwtUtil.extractExpiration(token).toInstant();

        tokenBlacklistRepository.save(
                TokenBlacklist.builder()
                        .token(token)
                        .expiresAt(expiresAt)
                        .revokedAt(Instant.now())
                        .build()
        );

        log.info("Token revoked, expires at {}", expiresAt);
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistRepository.existsByToken(token);
    }

    // Runs every hour — removes rows whose JWT expiry has already passed,
    // so the blacklist table doesn't grow unbounded.
    @Scheduled(fixedRateString = "PT1H")
    @Transactional
    public void purgeExpiredTokens() {
        tokenBlacklistRepository.deleteAllExpiredBefore(Instant.now());
        log.debug("Purged expired blacklisted tokens");
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}