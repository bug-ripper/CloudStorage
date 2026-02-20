package com.denisbondd111.metadataservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MetadataNotFoundException.class)
    public ResponseEntity<String> handle(MetadataNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}