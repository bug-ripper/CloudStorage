package com.denisbondd111.common.event;

public record FileDeletedEvent(
        String fileId,
        String userId
) {}
