package com.denisbondd111.storageservice.controller;
import com.denisbondd111.common.api.StorageApi;
import com.denisbondd111.common.dto.FileUploadResponse;
import com.denisbondd111.storageservice.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class StorageController implements StorageApi {

    private final StorageService storageService;

    @Override
    public FileUploadResponse upload(String userId, MultipartFile file) {
        return storageService.upload(userId, file);
    }

    @Override
    public ResponseEntity<InputStreamResource> download(String userId, String fileId) {

        var result = storageService.download(userId, fileId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.size())
                .body(new InputStreamResource(result.stream()));
    }


    @Override
    public void delete(String userId, String fileId) {
        storageService.delete(userId, fileId);
    }
}