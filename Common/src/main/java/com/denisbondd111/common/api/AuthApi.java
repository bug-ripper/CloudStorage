package com.denisbondd111.common.api;

import com.denisbondd111.common.dto.TokenResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

public interface AuthApi {

    @GetMapping("/internal/auth/validate")
    TokenResponse validate(
            @RequestHeader("Authorization") String bearerToken
    );
}
