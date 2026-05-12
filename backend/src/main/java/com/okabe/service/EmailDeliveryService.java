package com.okabe.service;

public interface EmailDeliveryService {

    void sendHtmlEmail(String to, String subject, String htmlContent);
}
