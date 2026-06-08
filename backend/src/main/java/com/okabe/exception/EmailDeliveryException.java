package com.okabe.exception;

public class EmailDeliveryException extends RuntimeException {
    // Exception ném ra khi gửi email thất bại

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause); // Truyền thông báo và nguyên nhân gốc lên RuntimeException
    }
}
