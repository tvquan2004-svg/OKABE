package com.okabe.exception;

public class DuplicateResourceException extends RuntimeException {
    // Exception ném ra khi tài nguyên bị trùng lặp (vd: email đã tồn tại)

    public DuplicateResourceException(String message) {
        super(message); // Truyền thông báo lỗi lên RuntimeException
    }
}
