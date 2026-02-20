package com.denisbondd111.metadataservice.exception;

public class MetadataNotFoundException extends RuntimeException {
    public MetadataNotFoundException(String message) {
        super(message);
    }
}