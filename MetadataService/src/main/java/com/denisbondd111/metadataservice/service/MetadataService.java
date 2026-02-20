package com.denisbondd111.metadataservice.service;

import com.denisbondd111.metadataservice.exception.MetadataNotFoundException;
import com.denisbondd111.metadataservice.model.FileMetadataEntity;
import com.denisbondd111.metadataservice.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final FileMetadataRepository repository;

    public List<FileMetadataEntity> getUserFiles(String userId) {
        return repository.findAllByUserId(userId);
    }

    public FileMetadataEntity getFile(String userId, String fileId) {
        return repository.findById(fileId)
                .filter(f -> f.getUserId().equals(userId))
                .orElseThrow(() -> new MetadataNotFoundException("File not found"));
    }
}
