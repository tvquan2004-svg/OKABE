package com.okabe.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.okabe.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@org.springframework.context.annotation.Primary
@ConditionalOnProperty(name = "app.storage.type", havingValue = "cloudinary")
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements StorageService {

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        log.info("Initializing Cloudinary with Cloud Name: {}", mask(cloudName)); // Ghi log thông tin khởi tạo
        
        if (cloudName == null || cloudName.isEmpty() ||  // Nếu thiếu cloud name
            apiKey == null || apiKey.isEmpty() ||  // Nếu thiếu api key
            apiSecret == null || apiSecret.isEmpty()) { // Nếu thiếu api secret
            log.error("Cloudinary credentials are missing! Environment variables CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET must be set."); // Ghi log lỗi
            return; // Thoát nếu thiếu thông tin
        }
        
        cloudinary = new Cloudinary(ObjectUtils.asMap( // Khởi tạo Cloudinary với các thông tin xác thực
            "cloud_name", cloudName, // Gán cloud name
            "api_key", apiKey, // Gán api key
            "api_secret", apiSecret, // Gán api secret
            "secure", true // Sử dụng HTTPS
        ));
        log.info("Cloudinary initialized successfully."); // Ghi log thành công
    }

    private String mask(String value) {
        if (value == null || value.length() < 4) return "****"; // Trả về **** nếu giá trị quá ngắn
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2); // Che giấu phần giữa của chuỗi
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        if (cloudinary == null) { // Nếu Cloudinary chưa được khởi tạo
            log.error("Upload attempted but Cloudinary is not configured!"); // Ghi log lỗi
            throw new IOException("Cloudinary is not configured. Please add CLOUDINARY_CLOUD_NAME, API_KEY and API_SECRET to your .env file."); // Ném lỗi
        }
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getInputStream(), ObjectUtils.asMap( // Tải file lên Cloudinary dạng stream
                "resource_type", "auto", // Tự động phát hiện loại tài nguyên
                "folder", "okabe/attachments" // Thư mục lưu trữ
            ));
            String url = (String) uploadResult.get("secure_url"); // Lấy URL an toàn từ kết quả
            log.info("File uploaded successfully to Cloudinary: {}", url); // Ghi log thành công
            return url; // Trả về URL
        } catch (Exception e) {
            log.error("Cloudinary upload failed: {}", e.getMessage()); // Ghi log lỗi
            throw new IOException("Cloudinary upload failed: " + e.getMessage(), e); // Ném lỗi
        }
    }

    @Override
    public void delete(String url) {
        try {
            // Extract public_id from URL
            // Example URL: https://res.cloudinary.com/demo/image/upload/v123456/okabe/attachments/sample.jpg
            String publicId = extractPublicId(url); // Trích xuất public_id từ URL
            if (publicId != null) { // Nếu lấy được public_id
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap()); // Xóa file trên Cloudinary
                log.info("File deleted from Cloudinary: {}", publicId); // Ghi log thành công
            }
        } catch (Exception e) {
            log.error("Cloudinary delete failed: {}", e.getMessage()); // Ghi log lỗi
        }
    }

    private String extractPublicId(String url) {
        try {
            // Very basic extraction logic
            // Works for standard Cloudinary URLs in the "okabe/attachments" folder
            if (url == null || !url.contains("okabe/attachments")) return null; // Trả về null nếu URL không hợp lệ
            int startIndex = url.indexOf("okabe/attachments"); // Tìm vị trí bắt đầu của public_id
            int endIndex = url.lastIndexOf("."); // Tìm vị trí kết thúc (dấu chấm cuối)
            return url.substring(startIndex, endIndex); // Trích xuất public_id
        } catch (Exception e) {
            return null; // Trả về null nếu có lỗi
        }
    }
}
