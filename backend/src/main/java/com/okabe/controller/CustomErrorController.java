package com.okabe.controller;

import com.okabe.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping(path = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String message = (String) request.getAttribute("jakarta.servlet.error.message");
        Throwable exception = (Throwable) request.getAttribute("jakarta.servlet.error.exception");

        if (statusCode == null) {
            statusCode = 500;
        }
        if (message == null || message.isBlank()) {
            message = switch (statusCode) {
                case 400 -> "Bad request";
                case 401 -> "Unauthorized";
                case 403 -> "Forbidden";
                case 404 -> "Resource not found";
                case 405 -> "Method not allowed";
                case 500 -> "Internal server error";
                case 502 -> "Bad gateway";
                case 503 -> "Service unavailable";
                default -> "Error";
            };
        }

        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(message, status.name()));
    }
}
