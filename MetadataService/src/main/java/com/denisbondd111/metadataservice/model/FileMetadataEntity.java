package com.denisbondd111.metadataservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "file_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadataEntity {

    @Id
    private String fileId;

    private String userId;

    private String objectKey;

    private String filename;

    private String contentType;

    private long size;

    private Instant uploadedAt;
}