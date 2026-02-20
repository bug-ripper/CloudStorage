package com.denisbondd111.common.error;

public record ErrorResponse(
        ErrorCode code,
        String message
) {}