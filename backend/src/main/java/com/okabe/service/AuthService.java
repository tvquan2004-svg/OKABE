package com.okabe.service;

import com.okabe.dto.request.LoginRequest;
import com.okabe.dto.request.RegisterRequest;
import com.okabe.dto.response.AuthResponse;
import com.okabe.security.UserPrincipal;

public interface AuthService {

    /**
     * Register a new user account.
     *
     * @param request registration data (username, email, password)
     * @return auth response with tokens and user info
     * @throws com.okabe.exception.DuplicateResourceException if email already exists
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate user with email and password.
     *
     * @param request login credentials
     * @return auth response with tokens and user info
     */
    AuthResponse login(LoginRequest request);

    /**
     * Authenticate user with Google ID token.
     *
     * @param request containing Google ID token
     * @return auth response with app tokens
     */
    AuthResponse googleLogin(com.okabe.dto.request.GoogleLoginRequest request);

    /**
     * Generate new access token using a valid refresh token.
     *
     * @param refreshToken the refresh token string
     * @return auth response with new tokens
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Get current authenticated user info.
     *
     * @param currentUser the authenticated user principal
     * @return auth response with user info (no tokens)
     */
    AuthResponse.UserInfo getCurrentUser(UserPrincipal currentUser);
}
