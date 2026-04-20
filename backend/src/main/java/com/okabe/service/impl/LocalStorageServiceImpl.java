package com.okabe.service.impl;

import com.okabe.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageServiceImpl implements StorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path root;

    @PostConstruct
    public void init() {
        try {
            root = Paths.get(uploadDir);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
                log.info("Created upload directory at: {}", root.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Could not initialize storage folder", e);
            throw new RuntimeException("Could not initialize storage folder", e);
        }
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IOException("Failed to store empty file.");
        
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        try {
            Files.copy(file.getInputStream(), this.root.resolve(filename));
            // Trả về URL để Frontend có thể gọi (Cần cấu hình Resource Handler)
            return "/api/v1/files/" + filename;
        } catch (Exception e) {
            throw new IOException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @Override
    public void delete(String url) {
        try {
            String filename = url.substring(url.lastIndexOf("/") + 1);
            Files.deleteIfExists(this.root.resolve(filename));
            log.info("Deleted file: {}", filename);
        } catch (IOException e) {
            log.error("Could not delete file", e);
        }
    }
}
