package com.denisbondd111.storageservice.exception;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<String> handle(StorageException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
