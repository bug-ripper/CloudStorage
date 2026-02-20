package com.denisbondd111.common.dto;

import java.time.Instant;
import java.util.Map;

public record FileMetadataDto(
        String fileId,
        String userId,
        String filename,
        String contentType,
        long size,
        Instant uploadedAt,
        Map<String, String> attributes
) {}
