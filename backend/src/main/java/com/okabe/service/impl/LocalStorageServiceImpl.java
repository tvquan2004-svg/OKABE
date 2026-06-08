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
            root = Paths.get(uploadDir); // Lấy đường dẫn thư mục upload
            if (!Files.exists(root)) { // Nếu thư mục chưa tồn tại
                Files.createDirectories(root); // Tạo thư mục upload
                log.info("Created upload directory at: {}", root.toAbsolutePath()); // Ghi log thông tin
            }
        } catch (IOException e) {
            log.error("Could not initialize storage folder", e); // Ghi log lỗi
            throw new RuntimeException("Could not initialize storage folder", e); // Ném lỗi runtime
        }
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IOException("Failed to store empty file."); // Ném lỗi nếu file rỗng
        
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename(); // Tạo tên file duy nhất
        try {
            Files.copy(file.getInputStream(), this.root.resolve(filename)); // Sao chép file vào thư mục upload
            // Trả về URL để Frontend có thể gọi (Cần cấu hình Resource Handler)
            return "/api/v1/files/" + filename; // Trả về đường dẫn URL
        } catch (Exception e) {
            throw new IOException("Could not store the file. Error: " + e.getMessage()); // Ném lỗi nếu không lưu được
        }
    }

    @Override
    public void delete(String url) {
        try {
            String filename = url.substring(url.lastIndexOf("/") + 1); // Trích xuất tên file từ URL
            Files.deleteIfExists(this.root.resolve(filename)); // Xóa file nếu tồn tại
            log.info("Deleted file: {}", filename); // Ghi log thông tin
        } catch (IOException e) {
            log.error("Could not delete file", e); // Ghi log lỗi
        }
    }
}
