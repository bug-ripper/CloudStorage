package com.denisbondd111.common.event;

import java.time.Instant;

public record FileUploadedEvent(
        String fileId,
        String userId,
        String storagePath,
        String contentType,
        String objectKey,
        String originalFilename,
        long size,
        Instant uploadedAt
) {}
