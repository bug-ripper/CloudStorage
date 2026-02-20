package com.denisbondd111.storageservice.service;


import com.denisbondd111.common.dto.FileDownloadResult;
import com.denisbondd111.common.dto.FileUploadResponse;
import com.denisbondd111.common.event.FileDeletedEvent;
import com.denisbondd111.common.event.FileUploadedEvent;
import com.denisbondd111.storageservice.exception.StorageException;
import com.denisbondd111.storageservice.kafka.FileEventProducer;
import com.denisbondd111.storageservice.util.S3KeyGenerator;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final FileEventProducer eventProducer;

    @Value("${minio.bucket}")
    private String bucket;

    public FileUploadResponse upload(String userId, MultipartFile file) {
        try {
            String fileId = UUID.randomUUID().toString();
            String objectName = S3KeyGenerator.generate(userId, fileId);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            eventProducer.fileUploaded(new FileUploadedEvent(
                    fileId,
                    userId,
                    objectName,
                    file.getContentType(),
                    userId + "/" + fileId,
                    file.getOriginalFilename(),
                    file.getSize(),
                    Instant.now()
            ));

            return new FileUploadResponse(
                    fileId,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType()
            );

        } catch (Exception e) {
            throw new StorageException("Failed to upload file", e);
        }
    }

    public FileDownloadResult download(String userId, String fileId) {
        try {
            String objectName = S3KeyGenerator.generate(userId, fileId);

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );

            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );

            return new FileDownloadResult(
                    stream,
                    stat.userMetadata().getOrDefault("original-filename", fileId),
                    stat.contentType(),
                    stat.size()
            );

        } catch (Exception e) {
            throw new StorageException("Failed to download file", e);
        }
    }


    public void delete(String userId, String fileId) {
        try {
            String objectName = S3KeyGenerator.generate(userId, fileId);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );

            eventProducer.fileDeleted(new FileDeletedEvent(
                    fileId,
                    userId
            ));

        } catch (Exception e) {
            throw new StorageException("Failed to delete file", e);
        }
    }
}