package com.okabe.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    // Upload file và trả về storage key hoặc URL
    String upload(MultipartFile file) throws IOException;

    // Xoá file theo storage key hoặc URL
    void delete(String key);
}
