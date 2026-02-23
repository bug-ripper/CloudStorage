package com.denisbondd111.authservice.controller;

import com.denisbondd111.authservice.domain.dto.AuthResponse;
import com.denisbondd111.authservice.domain.dto.LoginRequest;
import com.denisbondd111.authservice.domain.dto.RegisterRequest;
import com.denisbondd111.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestParam String refreshToken) {
        return authService.refresh(refreshToken);
    }
}
