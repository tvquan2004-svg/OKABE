package com.okabe.exception;

public class ResourceNotFoundException extends RuntimeException {
    // Exception ném ra khi không tìm thấy tài nguyên

    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with id: %d", resourceName, id)); // Tạo thông báo với tên resource và id
    }

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(String.format("%s not found with identifier: %s", resourceName, identifier)); // Tạo thông báo với tên resource và identifier
    }

    public ResourceNotFoundException(String message) {
        super(message); // Thông báo tuỳ chỉnh
    }
}
