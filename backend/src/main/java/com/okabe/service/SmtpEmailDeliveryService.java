package com.okabe.service;

import com.okabe.exception.EmailDeliveryException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailDeliveryService implements EmailDeliveryService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:${spring.mail.username:noreply@okabe.com}}")
    private String fromEmail;

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String sender = StringUtils.hasText(fromEmail) ? fromEmail : "noreply@okabe.com";
            helper.setFrom(sender);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            log.info("[EMAIL][SMTP] Sending email to {}", to);
            mailSender.send(message);
            log.info("[EMAIL][SMTP] Email sent to {}", to);
        } catch (Exception e) {
            throw new EmailDeliveryException("SMTP email delivery failed for " + to, e);
        }
    }
}
