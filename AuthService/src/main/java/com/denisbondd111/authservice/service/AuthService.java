package com.denisbondd111.authservice.service;

import com.denisbondd111.authservice.config.JwtConfig;
import com.denisbondd111.authservice.domain.dto.AuthResponse;
import com.denisbondd111.authservice.domain.dto.LoginRequest;
import com.denisbondd111.authservice.domain.dto.RegisterRequest;
import com.denisbondd111.authservice.domain.entity.Role;
import com.denisbondd111.authservice.domain.entity.User;
import com.denisbondd111.authservice.repository.RoleRepository;
import com.denisbondd111.authservice.repository.UserRepository;
import com.denisbondd111.authservice.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TokenService tokenService;
    private final JwtConfig jwtConfig;   // 👈 добавили

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow();

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .createdAt(Instant.now())
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);

        return generateTokens(user);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return generateTokens(user);
    }

    public AuthResponse   refresh(String refreshToken) {

        if (!tokenService.isRefreshTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        var userId = jwtUtils.extractUserId(refreshToken);
        User user = userRepository.findById(userId).orElseThrow();

        tokenService.deleteRefreshToken(refreshToken);

        return generateTokens(user);
    }

    private AuthResponse generateTokens(User user) {

        String access = jwtUtils.generateAccessToken(
                user.getId(),
                user.getEmail(),
                Map.of("roles", user.getRoles().stream()
                        .map(Role::getName)
                        .toList())
        );

        String refresh = jwtUtils.generateRefreshToken(user.getId());

        tokenService.storeRefreshToken(
                refresh,
                jwtConfig.getRefreshTokenExpiration()
        );

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .build();
    }
}