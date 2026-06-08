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
        if (userRepository.existsByEmail(request.email())) { // Kiểm tra email đã tồn tại
            throw new DuplicateResourceException("Email already registered: " + request.email()); // Ném lỗi nếu email đã đăng ký
        }

        User user = User.builder()
                .username(request.username()) // Gán tên người dùng
                .email(request.email()) // Gán email
                .password(passwordEncoder.encode(request.password())) // Mã hóa mật khẩu
                .isActive(false) // Đặt trạng thái chưa kích hoạt
                .build(); // Xây dựng đối tượng User

        user = userRepository.save(user); // Lưu người dùng vào CSDL
        log.info("New user registered (inactive): {}", user.getEmail()); // Ghi log thông tin

        String token = UUID.randomUUID().toString(); // Tạo token xác thực ngẫu nhiên
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token) // Gán token
                .user(user) // Gán người dùng
                .expiryDate(LocalDateTime.now().plusHours(24)) // Đặt thời gian hết hạn 24h
                .build(); // Xây dựng đối tượng EmailVerificationToken
        tokenRepository.save(verificationToken); // Lưu token vào CSDL

        emailNotificationService.sendEmailVerification(user, token); // Gửi email xác thực

        return AuthResponse.builder()
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId()) // Gán ID người dùng
                        .email(user.getEmail()) // Gán email
                        .username(user.getUsername()) // Gán tên người dùng
                        .build()) // Xây dựng UserInfo
                .build(); // Xây dựng và trả về AuthResponse
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()) // Tìm user theo email
                .orElseThrow(() -> new UnauthorizedException("Bad credentials")); // Ném lỗi nếu không tìm thấy

        if (!user.getIsActive()) { // Nếu tài khoản chưa kích hoạt
            throw new UnauthorizedException("Please verify your email before logging in."); // Ném lỗi yêu cầu xác thực email
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())); // Xác thực thông tin đăng nhập

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal(); // Lấy thông tin principal từ authentication
            log.info("User logged in: {}", user.getEmail()); // Ghi log thông tin đăng nhập
            
            if (user.getIs2faEnabled()) { // Nếu người dùng bật xác thực 2 yếu tố
                String tempToken = jwtTokenProvider.generateTempToken(userPrincipal); // Tạo token tạm thời
                return AuthResponse.builder()
                        .requires2fa(true) // Đánh dấu yêu cầu 2FA
                        .tempToken(tempToken) // Gán token tạm thời
                        .build(); // Xây dựng và trả về AuthResponse
            }
            
            return buildAuthResponse(userPrincipal, user); // Trả về phản hồi xác thực đầy đủ
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Bad credentials"); // Ném lỗi thông tin đăng nhập sai
        }
    }

    @Value("${app.google.client-id:YOUR_GOOGLE_CLIENT_ID}")
    private String googleClientId; // Google Client ID từ cấu hình

    @Override
    @Transactional
    public AuthResponse googleLogin(com.okabe.dto.request.GoogleLoginRequest request) {
        try {
            String email; // Biến lưu email Google
            String name; // Biến lưu tên Google
            String pictureUrl; // Biến lưu URL ảnh đại diện

            if (request.idToken() != null && !request.idToken().isBlank()) { // Nếu có idToken
                com.google.api.client.http.HttpTransport transport = new com.google.api.client.http.javanet.NetHttpTransport(); // Tạo HTTP transport
                com.google.api.client.json.JsonFactory jsonFactory = new com.google.api.client.json.gson.GsonFactory(); // Tạo JSON factory

                com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier =
                        new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                                .setAudience(java.util.Collections.singletonList(googleClientId)) // Đặt audience
                                .build(); // Xây dựng GoogleIdTokenVerifier

                com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = verifier.verify(request.idToken()); // Xác thực idToken
                if (idToken == null) throw new IllegalArgumentException("Invalid Google ID token"); // Ném lỗi nếu token không hợp lệ
                
                com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload(); // Lấy payload từ token
                email = payload.getEmail(); // Lấy email từ payload
                name = (String) payload.get("name"); // Lấy tên từ payload
                pictureUrl = (String) payload.get("picture"); // Lấy ảnh đại diện từ payload
            } else if (request.accessToken() != null && !request.accessToken().isBlank()) { // Nếu có accessToken
                // Fetch from userinfo endpoint
                String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + request.accessToken(); // Tạo URL lấy thông tin user
                java.util.Map<String, Object> userInfo = restTemplate.getForObject(url, java.util.Map.class); // Gọi API Google để lấy thông tin
                if (userInfo == null) throw new IllegalArgumentException("Invalid Google access token"); // Ném lỗi nếu không lấy được thông tin
                
                email = (String) userInfo.get("email"); // Lấy email từ response
                name = (String) userInfo.get("name"); // Lấy tên từ response
                pictureUrl = (String) userInfo.get("picture"); // Lấy ảnh đại diện từ response
            } else { // Nếu không có cả idToken và accessToken
                throw new IllegalArgumentException("Either idToken or accessToken is required"); // Ném lỗi yêu cầu token
            }

            User user = userRepository.findByEmail(email).orElse(null); // Tìm user theo email
            
            if (user == null) { // Nếu user chưa tồn tại
                // Check if it's a registration call (username provided)
                if (request.username() != null && !request.username().isBlank()) { // Nếu có username (đăng ký)
                    user = User.builder()
                            .email(email) // Gán email
                            .username(request.username()) // Gán username
                            .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Tạo mật khẩu ngẫu nhiên
                            .avatarUrl(pictureUrl) // Gán ảnh đại diện
                            .provider("GOOGLE") // Đánh dấu nhà cung cấp Google
                            .isActive(true) // Kích hoạt tài khoản
                            .build(); // Xây dựng đối tượng User
                    user = userRepository.save(user); // Lưu user mới
                    log.info("New Google user registered via confirmation: {}", email); // Ghi log
                } else { // Nếu chưa có username, yêu cầu xác nhận
                    // Don't save yet, ask for confirmation/username
                    return AuthResponse.builder()
                            .needsRegistration(true) // Đánh dấu cần đăng ký
                            .email(email) // Gán email
                            .avatarUrl(pictureUrl) // Gán ảnh đại diện
                            .googleName(name) // Gán tên Google
                            .build(); // Trả về phản hồi
                }
            } else { // Nếu user đã tồn tại
                // User exists, log them in
                if (user.getAvatarUrl() == null && pictureUrl != null) { // Nếu chưa có ảnh đại diện
                    user.setAvatarUrl(pictureUrl); // Cập nhật ảnh đại diện
                    user = userRepository.save(user); // Lưu thay đổi
                }
                log.info("Google user logged in: {}", email); // Ghi log đăng nhập
            }

            UserPrincipal userPrincipal = UserPrincipal.from(user); // Tạo UserPrincipal từ user
            if (user.getIs2faEnabled()) { // Nếu bật 2FA
                String tempToken = jwtTokenProvider.generateTempToken(userPrincipal); // Tạo token tạm thời
                return AuthResponse.builder()
                        .requires2fa(true) // Đánh dấu yêu cầu 2FA
                        .tempToken(tempToken) // Gán token tạm thời
                        .build(); // Xây dựng AuthResponse
            }
            return buildAuthResponse(userPrincipal, user); // Trả về phản hồi xác thực
        } catch (Exception e) {
            log.error("Google login failed", e); // Ghi log lỗi
            throw new IllegalArgumentException("Google login failed: " + e.getMessage()); // Ném lỗi
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) { // Kiểm tra token hợp lệ
            throw new IllegalArgumentException("Invalid refresh token"); // Ném lỗi nếu token không hợp lệ
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken); // Lấy loại token
        if (!"refresh".equals(tokenType)) { // Nếu không phải refresh token
            throw new IllegalArgumentException("Token is not a refresh token"); // Ném lỗi
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken); // Lấy userId từ token
        User user = userRepository.findById(userId) // Tìm user theo ID
                .orElseThrow(() -> new ResourceNotFoundException("User", userId)); // Ném lỗi nếu không tìm thấy

        UserPrincipal userPrincipal = UserPrincipal.from(user); // Tạo UserPrincipal
        return buildAuthResponse(userPrincipal, user); // Trả về phản hồi xác thực mới
    }

    @Override
    public AuthResponse.UserInfo getCurrentUser(UserPrincipal currentUser) {
        if (currentUser == null) return null; // Trả về null nếu chưa đăng nhập
        return AuthResponse.UserInfo.builder()
                .id(currentUser.getId()) // Gán ID người dùng
                .email(currentUser.getEmail()) // Gán email
                .username(currentUser.getUsername()) // Gán tên người dùng
                .is2faEnabled(currentUser.is2faEnabled()) // Gán trạng thái 2FA
                .build(); // Xây dựng UserInfo
    }

    private AuthResponse buildAuthResponse(UserPrincipal userPrincipal, User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal); // Tạo access token
        String refreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal); // Tạo refresh token

        return AuthResponse.builder()
                .accessToken(accessToken) // Gán access token
                .refreshToken(refreshToken) // Gán refresh token
                .tokenType("Bearer") // Đặt loại token là Bearer
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId()) // Gán ID người dùng
                        .email(user.getEmail()) // Gán email
                        .username(user.getUsername()) // Gán tên người dùng
                        .avatarUrl(user.getAvatarUrl()) // Gán URL ảnh đại diện
                        .is2faEnabled(user.getIs2faEnabled()) // Gán trạng thái 2FA
                        .build()) // Xây dựng UserInfo
                .build(); // Xây dựng AuthResponse
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token) // Tìm token xác thực
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token")); // Ném lỗi nếu token không hợp lệ

        if (verificationToken.isExpired()) { // Nếu token đã hết hạn
            throw new IllegalArgumentException("Verification token has expired"); // Ném lỗi token hết hạn
        }

        User user = verificationToken.getUser(); // Lấy user từ token
        user.setIsActive(true); // Kích hoạt tài khoản
        userRepository.save(user); // Lưu thay đổi

        tokenRepository.delete(verificationToken); // Xóa token xác thực
        log.info("User email verified: {}", user.getEmail()); // Ghi log thông tin
    }
}
