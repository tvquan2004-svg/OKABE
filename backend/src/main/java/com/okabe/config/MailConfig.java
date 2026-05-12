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
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailProperties.getHost());
        mailSender.setPort(mailProperties.getPort());
        mailSender.setUsername(mailProperties.getUsername());
        mailSender.setPassword(normalizePassword(mailProperties));
        mailSender.setProtocol(mailProperties.getProtocol());
        Charset defaultEncoding = mailProperties.getDefaultEncoding();
        if (defaultEncoding != null) {
            mailSender.setDefaultEncoding(defaultEncoding.name());
        }
        mailSender.setJavaMailProperties(toJavaMailProperties(mailProperties));
        return mailSender;
    }

    private Properties toJavaMailProperties(MailProperties mailProperties) {
        Properties properties = new Properties();
        properties.putAll(mailProperties.getProperties());
        return properties;
    }

    private String normalizePassword(MailProperties mailProperties) {
        String password = mailProperties.getPassword();
        if (password == null || !GMAIL_SMTP_HOST.equalsIgnoreCase(mailProperties.getHost())) {
            return password;
        }

        String normalizedPassword = password.replaceAll("\\s+", "");
        if (!password.equals(normalizedPassword)) {
            log.warn("Gmail SMTP app password contained whitespace and was normalized before use.");
        }
        return normalizedPassword;
    }
}
