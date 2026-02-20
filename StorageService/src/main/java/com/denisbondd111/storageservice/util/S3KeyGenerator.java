package com.denisbondd111.storageservice.util;

public final class S3KeyGenerator {

    private S3KeyGenerator() {}

    public static String generate(String userId, String fileId) {
        return userId + "/" + fileId;
    }
}
