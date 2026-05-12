package com.okabe.service;

import com.okabe.exception.EmailDeliveryException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendEmailDeliveryService implements EmailDeliveryService {

    private static final String RESEND_EMAILS_ENDPOINT = "https://api.resend.com/emails";

    private final RestClient.Builder restClientBuilder;

    @Value("${app.email.resend.api-key:}")
    private String apiKey;

    @Value("${app.email.from:}")
    private String fromEmail;

    private RestClient restClient;

    @PostConstruct
    void init() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("RESEND_API_KEY is required when EMAIL_PROVIDER=resend");
        }
        if (!StringUtils.hasText(fromEmail)) {
            throw new IllegalStateException("MAIL_FROM is required when EMAIL_PROVIDER=resend");
        }

        restClient = restClientBuilder
                .baseUrl(RESEND_EMAILS_ENDPOINT)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("[EMAIL][RESEND] Resend email provider initialized with sender {}", fromEmail);
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            ResendEmailRequest request = new ResendEmailRequest(
                    fromEmail,
                    List.of(to),
                    subject,
                    htmlContent
            );

            log.info("[EMAIL][RESEND] Sending email to {}", to);
            ResponseEntity<String> response = restClient.post()
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new EmailDeliveryException(
                        "Resend API returned status " + response.getStatusCode().value(),
                        null
                );
            }

            log.info("[EMAIL][RESEND] Email accepted by Resend for {}", to);
        } catch (EmailDeliveryException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailDeliveryException("Resend email delivery failed for " + to, e);
        }
    }

    private record ResendEmailRequest(
            String from,
            List<String> to,
            String subject,
            String html
    ) {
    }
}
