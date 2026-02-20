package com.denisbondd111.metadataservice.controller;


import com.denisbondd111.metadataservice.model.FileMetadataEntity;
import com.denisbondd111.metadataservice.service.MetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataService metadataService;

    @GetMapping("/files")
    public List<FileMetadataEntity> list(
            @RequestHeader("X-User-Id") String userId
    ) {
        return metadataService.getUserFiles(userId);
    }

    @GetMapping("/files/{fileId}")
    public FileMetadataEntity get(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String fileId
    ) {
        return metadataService.getFile(userId, fileId);
    }
}