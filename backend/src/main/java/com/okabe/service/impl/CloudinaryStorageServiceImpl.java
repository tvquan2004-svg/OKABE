package com.okabe.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.okabe.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@org.springframework.context.annotation.Primary
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
        log.info("Initializing Cloudinary with Cloud Name: {}", mask(cloudName));
        
        if (cloudName == null || cloudName.isEmpty() || 
            apiKey == null || apiKey.isEmpty() || 
            apiSecret == null || apiSecret.isEmpty()) {
            log.error("Cloudinary credentials are missing! Environment variables CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET must be set.");
            return;
        }
        
        cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ));
        log.info("Cloudinary initialized successfully.");
    }

    private String mask(String value) {
        if (value == null || value.length() < 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        if (cloudinary == null) {
            log.error("Upload attempted but Cloudinary is not configured!");
            throw new IOException("Cloudinary is not configured. Please add CLOUDINARY_CLOUD_NAME, API_KEY and API_SECRET to your .env file.");
        }
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "okabe/attachments"
            ));
            String url = (String) uploadResult.get("secure_url");
            log.info("File uploaded successfully to Cloudinary: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new IOException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String url) {
        try {
            // Extract public_id from URL
            // Example URL: https://res.cloudinary.com/demo/image/upload/v123456/okabe/attachments/sample.jpg
            String publicId = extractPublicId(url);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("File deleted from Cloudinary: {}", publicId);
            }
        } catch (Exception e) {
            log.error("Cloudinary delete failed: {}", e.getMessage());
        }
    }

    private String extractPublicId(String url) {
        try {
            // Very basic extraction logic
            // Works for standard Cloudinary URLs in the "okabe/attachments" folder
            if (url == null || !url.contains("okabe/attachments")) return null;
            int startIndex = url.indexOf("okabe/attachments");
            int endIndex = url.lastIndexOf(".");
            return url.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }
}
