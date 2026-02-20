package com.denisbondd111.common.api;

import com.denisbondd111.common.dto.FileMetadataDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

public interface MetadataApi {

    @PostMapping("/internal/metadata")
    void save(FileMetadataDto metadata);

    @GetMapping("/internal/metadata/{fileId}")
    FileMetadataDto get(@PathVariable String fileId);
}