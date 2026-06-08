package com.okabe.exception;

public class UnauthorizedException extends RuntimeException {
    // Exception ném ra khi người dùng không có quyền truy cập

    public UnauthorizedException(String message) {
        super(message); // Truyền thông báo lỗi lên RuntimeException
    }
}
