package com.denisbondd111.metadataservice.repository;

import com.denisbondd111.metadataservice.model.FileMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, String> {

    List<FileMetadataEntity> findAllByUserId(String userId);
}
