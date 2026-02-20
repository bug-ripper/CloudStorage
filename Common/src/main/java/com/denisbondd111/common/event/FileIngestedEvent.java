package com.denisbondd111.common.event;

import java.time.Instant;

public record FileIngestedEvent(
        String fileId,
        String userId,
        String contentType,
        String textStorageKey,   // <---
        long textSize,
        Instant ingestedAt
) {}
