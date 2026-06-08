package com.okabe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration(); // Tạo cấu hình CORS
        // Cho phép tất cả các nguồn nhưng vẫn giữ được tính năng gửi Credentials (Token/Cookie)
        config.setAllowedOriginPatterns(List.of("*")); // Cho phép mọi origin pattern
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")); // Các phương thức HTTP được phép
        config.setAllowedHeaders(List.of("*")); // Cho phép tất cả header
        config.setAllowCredentials(true); // Cho phép gửi cookie/token qua CORS
        config.setMaxAge(3600L); // Cache preflight request trong 1 giờ

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); // Nguồn cấu hình CORS dựa trên URL
        source.registerCorsConfiguration("/**", config); // Áp dụng cấu hình cho tất cả đường dẫn
        return source;
    }
}
