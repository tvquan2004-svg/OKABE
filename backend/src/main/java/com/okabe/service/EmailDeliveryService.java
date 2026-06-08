package com.okabe.service;

public interface EmailDeliveryService {

    // Gửi email HTML đến người nhận
    void sendHtmlEmail(String to, String subject, String htmlContent);
}
