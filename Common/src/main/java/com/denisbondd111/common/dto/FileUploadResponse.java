package com.denisbondd111.common.dto;

public record FileUploadResponse(
        String fileId,
        String originalFilename,
        long size,
        String contentType
) {}
