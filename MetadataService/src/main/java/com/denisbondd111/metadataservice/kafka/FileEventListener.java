package com.denisbondd111.metadataservice.kafka;

import com.denisbondd111.common.event.FileDeletedEvent;
import com.denisbondd111.common.event.FileUploadedEvent;
import com.denisbondd111.metadataservice.model.FileMetadataEntity;
import com.denisbondd111.metadataservice.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileEventListener {

    private final FileMetadataRepository repository;

    @KafkaListener(topics = "file.uploaded")
    public void handleUpload(FileUploadedEvent event) {

        FileMetadataEntity entity = FileMetadataEntity.builder()
                .fileId(event.fileId())
                .userId(event.userId())
                .objectKey(event.objectKey())
                .filename(event.originalFilename())
                .contentType(event.contentType())
                .size(event.size())
                .uploadedAt(event.uploadedAt())
                .build();

        repository.save(entity);
    }

    @KafkaListener(topics = "file.deleted")
    public void handleDelete(FileDeletedEvent event) {
        repository.deleteById(event.fileId());
    }
}
