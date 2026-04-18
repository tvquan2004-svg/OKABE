package com.okabe.service.impl;

import com.okabe.dto.request.LoginRequest;
import com.okabe.dto.request.RegisterRequest;
import com.okabe.dto.response.AuthResponse;
import com.okabe.entity.User;
import com.okabe.exception.DuplicateResourceException;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.repository.UserRepository;
import com.okabe.security.JwtTokenProvider;
import com.okabe.security.UserPrincipal;
import com.okabe.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        UserPrincipal userPrincipal = UserPrincipal.from(user);
        return buildAuthResponse(userPrincipal, user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", userPrincipal.getId()));

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(userPrincipal, user);
    }

    @org.springframework.beans.factory.annotation.Value("${app.google.client-id:YOUR_GOOGLE_CLIENT_ID}")
    private String googleClientId;

    @Override
    @Transactional
    public AuthResponse googleLogin(com.okabe.dto.request.GoogleLoginRequest request) {
        try {
            com.google.api.client.http.HttpTransport transport = new com.google.api.client.http.javanet.NetHttpTransport();
            com.google.api.client.json.JsonFactory jsonFactory = new com.google.api.client.json.gson.GsonFactory();

            com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier =
                    new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                            .setAudience(java.util.Collections.singletonList(googleClientId))
                            .build();

            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = verifier.verify(request.idToken());
            if (idToken != null) {
                com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();

                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");

                User user = userRepository.findByEmail(email).orElse(null);
                
                if (user == null) {
                    // Create new user for Google Login
                    user = User.builder()
                            .email(email)
                            .username(name != null ? name : email.split("@")[0])
                            .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                            .avatarUrl(pictureUrl)
                            .provider("GOOGLE")
                            .isActive(true)
                            .build();
                    user = userRepository.save(user);
                    log.info("New Google user registered: {}", email);
                } else {
                    // Update avatar if missing or changed, optional
                    if (user.getAvatarUrl() == null && pictureUrl != null) {
                        user.setAvatarUrl(pictureUrl);
                        user = userRepository.save(user);
                    }
                    log.info("Google user logged in: {}", email);
                }

                UserPrincipal userPrincipal = UserPrincipal.from(user);
                return buildAuthResponse(userPrincipal, user);
            } else {
                throw new IllegalArgumentException("Invalid Google ID token");
            }
        } catch (Exception e) {
            log.error("Google login failed", e);
            throw new IllegalArgumentException("Google login failed: " + e.getMessage());
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        UserPrincipal userPrincipal = UserPrincipal.from(user);
        return buildAuthResponse(userPrincipal, user);
    }

    @Override
    public AuthResponse.UserInfo getCurrentUser(UserPrincipal currentUser) {
        return AuthResponse.UserInfo.builder()
                .id(currentUser.getId())
                .email(currentUser.getEmail())
                .username(currentUser.getUsername())
                .build();
    }

    private AuthResponse buildAuthResponse(UserPrincipal userPrincipal, User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .build();
    }
}
