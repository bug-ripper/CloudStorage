package com.denisbondd111.common.api;


import com.denisbondd111.common.dto.FileUploadResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RequestMapping("/api/storage")
public interface StorageApi {

    @PostMapping("/upload")
    FileUploadResponse upload(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("file") MultipartFile file
    );


    @GetMapping("/download/{fileId}")
    ResponseEntity<InputStreamResource> download(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String fileId
    );

    @DeleteMapping("/{fileId}")
    void delete(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String fileId
    );
}
