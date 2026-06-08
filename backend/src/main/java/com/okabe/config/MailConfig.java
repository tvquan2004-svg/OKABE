package com.okabe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.Charset;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
@Slf4j
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
public class MailConfig {

    private static final String GMAIL_SMTP_HOST = "smtp.gmail.com";

    @Bean
    public JavaMailSender javaMailSender(MailProperties mailProperties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl(); // Tạo đối tượng gửi mail JavaMail
        mailSender.setHost(mailProperties.getHost()); // Đặt host SMTP từ cấu hình
        mailSender.setPort(mailProperties.getPort()); // Đặt cổng SMTP từ cấu hình
        mailSender.setUsername(mailProperties.getUsername()); // Đặt tên đăng nhập SMTP
        mailSender.setPassword(normalizePassword(mailProperties)); // Đặt mật khẩu (đã chuẩn hoá)
        mailSender.setProtocol(mailProperties.getProtocol()); // Đặt giao thức (smtp/smtps)
        Charset defaultEncoding = mailProperties.getDefaultEncoding(); // Lấy encoding mặc định
        if (defaultEncoding != null) { // Nếu có cấu hình encoding
            mailSender.setDefaultEncoding(defaultEncoding.name()); // Đặt encoding cho mail
        }
        mailSender.setJavaMailProperties(toJavaMailProperties(mailProperties)); // Chuyển các thuộc tính mail bổ sung
        return mailSender;
    }

    private Properties toJavaMailProperties(MailProperties mailProperties) {
        Properties properties = new Properties(); // Tạo Properties để chứa cấu hình JavaMail
        properties.putAll(mailProperties.getProperties()); // Copy tất cả thuộc tính từ MailProperties
        return properties;
    }

    private String normalizePassword(MailProperties mailProperties) {
        String password = mailProperties.getPassword(); // Lấy mật khẩu gốc
        if (password == null || !GMAIL_SMTP_HOST.equalsIgnoreCase(mailProperties.getHost())) {
            return password; // Không xử lý nếu password null hoặc không phải Gmail
        }

        String normalizedPassword = password.replaceAll("\\s+", ""); // Loại bỏ khoảng trắng trong mật khẩu
        if (!password.equals(normalizedPassword)) { // Nếu mật khẩu có chứa khoảng trắng
            log.warn("Gmail SMTP app password contained whitespace and was normalized before use.");
        }
        return normalizedPassword;
    }
}
