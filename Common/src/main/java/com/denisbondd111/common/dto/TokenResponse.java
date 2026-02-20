package com.denisbondd111.common.dto;

public record TokenResponse(
        String userId,
        boolean valid
) {}
