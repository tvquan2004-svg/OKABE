package com.okabe;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class OkabeApplication {

    public static void main(String[] args) {
        // 1. Try to load .env from current directory
        Dotenv dotenv = Dotenv.configure() // Cấu hình Dotenv
                .ignoreIfMissing() // Bỏ qua nếu file .env không tồn tại
                .load(); // Nạp các biến môi trường từ file .env
        
        // 2. If essential key is missing, try parent directory
        if (dotenv.get("CLOUDINARY_CLOUD_NAME") == null) { // Nếu thiếu biến quan trọng
            dotenv = Dotenv.configure()
                    .directory("../") // Thử load từ thư mục cha
                    .ignoreIfMissing()
                    .load();
        }

        dotenv.entries().forEach(entry -> { // Duyệt tất cả biến môi trường từ .env
            if (System.getProperty(entry.getKey()) == null) { // Chỉ set nếu chưa có trong system properties
                System.setProperty(entry.getKey(), entry.getValue()); // Set system property
            }
        });

        SpringApplication.run(OkabeApplication.class, args); // Khởi động Spring Boot
    }
}
