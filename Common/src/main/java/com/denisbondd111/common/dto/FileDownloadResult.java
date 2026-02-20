package com.denisbondd111.common.dto;

import java.io.InputStream;

public record FileDownloadResult(
        InputStream stream,
        String filename,
        String contentType,
        long size
) {}
