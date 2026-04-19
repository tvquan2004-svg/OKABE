package com.okabe.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    /**
     * Uploads a file and returns its storage key/URL.
     * @param file The file to upload.
     * @return The storage key or URL.
     * @throws IOException If upload fails.
     */
    String upload(MultipartFile file) throws IOException;

    /**
     * Deletes a file by its storage key/URL.
     * @param key The storage key or URL.
     */
    void delete(String key);
}
