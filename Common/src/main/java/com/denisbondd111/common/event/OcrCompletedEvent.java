package com.denisbondd111.common.event;

public record OcrCompletedEvent(
        String fileId,
        String userId,
        String extractedText
) {}
