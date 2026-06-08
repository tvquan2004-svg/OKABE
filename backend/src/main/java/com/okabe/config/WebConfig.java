package com.okabe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cấu hình để Spring Boot có thể phục vụ các file tĩnh từ thư mục uploads
        // URL sẽ có dạng: http://localhost:8080/api/v1/files/tên-file.jpg
        Path uploadPath = Paths.get(uploadDir); // Lấy đường dẫn thư mục upload từ cấu hình
        String absolutePath = uploadPath.toFile().getAbsolutePath(); // Chuyển sang đường dẫn tuyệt đối
        
        registry.addResourceHandler("/api/v1/files/**") // URL pattern để truy cập file
                .addResourceLocations("file:/" + absolutePath + "/"); // Đường dẫn vật lý chứa file
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(); // Tạo bean RestTemplate để gọi API bên ngoài
    }
}
