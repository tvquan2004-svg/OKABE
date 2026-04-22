package com.okabe.service.impl;

import com.okabe.dto.request.LoginRequest;
import com.okabe.dto.request.RegisterRequest;
import com.okabe.dto.response.AuthResponse;
import com.okabe.entity.User;
import com.okabe.entity.EmailVerificationToken;
import com.okabe.exception.DuplicateResourceException;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.UserRepository;
import com.okabe.repository.EmailVerificationTokenRepository;
import com.okabe.security.JwtTokenProvider;
import com.okabe.security.UserPrincipal;
import com.okabe.service.AuthService;
import com.okabe.service.EmailNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailNotificationService emailNotificationService;
    private final org.springframework.web.client.RestTemplate restTemplate;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            @Lazy AuthenticationManager authenticationManager,
            EmailVerificationTokenRepository tokenRepository,
            @Lazy EmailNotificationService emailNotificationService,
            org.springframework.web.client.RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.tokenRepository = tokenRepository;
        this.emailNotificationService = emailNotificationService;
        this.restTemplate = restTemplate;
    }

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
                .isActive(false)
                .build();

        user = userRepository.save(user);
        log.info("New user registered (inactive): {}", user.getEmail());

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        tokenRepository.save(verificationToken);

        emailNotificationService.sendEmailVerification(user, token);

        return AuthResponse.builder()
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .build())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Bad credentials"));

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Please verify your email before logging in.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            log.info("User logged in: {}", user.getEmail());
            return buildAuthResponse(userPrincipal, user);
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Bad credentials");
        }
    }

    @Value("${app.google.client-id:YOUR_GOOGLE_CLIENT_ID}")
    private String googleClientId;

    @Override
    @Transactional
    public AuthResponse googleLogin(com.okabe.dto.request.GoogleLoginRequest request) {
        try {
            String email;
            String name;
            String pictureUrl;

            if (request.idToken() != null && !request.idToken().isBlank()) {
                com.google.api.client.http.HttpTransport transport = new com.google.api.client.http.javanet.NetHttpTransport();
                com.google.api.client.json.JsonFactory jsonFactory = new com.google.api.client.json.gson.GsonFactory();

                com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier =
                        new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                                .setAudience(java.util.Collections.singletonList(googleClientId))
                                .build();

                com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = verifier.verify(request.idToken());
                if (idToken == null) throw new IllegalArgumentException("Invalid Google ID token");
                
                com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();
                email = payload.getEmail();
                name = (String) payload.get("name");
                pictureUrl = (String) payload.get("picture");
            } else if (request.accessToken() != null && !request.accessToken().isBlank()) {
                // Fetch from userinfo endpoint
                String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + request.accessToken();
                java.util.Map<String, Object> userInfo = restTemplate.getForObject(url, java.util.Map.class);
                if (userInfo == null) throw new IllegalArgumentException("Invalid Google access token");
                
                email = (String) userInfo.get("email");
                name = (String) userInfo.get("name");
                pictureUrl = (String) userInfo.get("picture");
            } else {
                throw new IllegalArgumentException("Either idToken or accessToken is required");
            }

            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                // Check if it's a registration call (username provided)
                if (request.username() != null && !request.username().isBlank()) {
                    user = User.builder()
                            .email(email)
                            .username(request.username())
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .avatarUrl(pictureUrl)
                            .provider("GOOGLE")
                            .isActive(true)
                            .build();
                    user = userRepository.save(user);
                    log.info("New Google user registered via confirmation: {}", email);
                } else {
                    // Don't save yet, ask for confirmation/username
                    return AuthResponse.builder()
                            .needsRegistration(true)
                            .email(email)
                            .avatarUrl(pictureUrl)
                            .googleName(name)
                            .build();
                }
            } else {
                // User exists, log them in
                if (user.getAvatarUrl() == null && pictureUrl != null) {
                    user.setAvatarUrl(pictureUrl);
                    user = userRepository.save(user);
                }
                log.info("Google user logged in: {}", email);
            }

            UserPrincipal userPrincipal = UserPrincipal.from(user);
            return buildAuthResponse(userPrincipal, user);
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

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setIsActive(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);
        log.info("User email verified: {}", user.getEmail());
    }
}
