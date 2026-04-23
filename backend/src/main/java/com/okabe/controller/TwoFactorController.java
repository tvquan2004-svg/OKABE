package com.okabe.controller;

import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.AuthResponse;
import com.okabe.entity.User;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.UserRepository;
import com.okabe.security.JwtTokenProvider;
import com.okabe.security.UserPrincipal;
import com.okabe.service.TwoFactorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/2fa")
@RequiredArgsConstructor
@Tag(name = "Two-Factor Authentication", description = "Endpoints for 2FA setup and validation")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup(@AuthenticationPrincipal UserPrincipal currentUser) {
        if (currentUser == null) throw new UnauthorizedException("Chưa đăng nhập");
        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        String secret = twoFactorService.generateNewSecret();
        String qrCodeUri = twoFactorService.getQrCodeUri(secret, user.getEmail());
        return ResponseEntity.ok(ApiResponse.success(new TwoFactorSetupResponse(secret, qrCodeUri)));
    }

    @PostMapping("/verify-setup")
    public ResponseEntity<ApiResponse<List<String>>> verifySetup(@AuthenticationPrincipal UserPrincipal currentUser, @RequestBody TwoFactorVerifyRequest request) {
        if (currentUser == null) throw new UnauthorizedException("Chưa đăng nhập");
        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        boolean isValid = twoFactorService.verifyCode(request.getSecret(), request.getCode());
        
        if (!isValid) throw new UnauthorizedException("Mã xác thực không hợp lệ");

        user.setTotpSecret(request.getSecret());
        user.setIs2faEnabled(true);
        userRepository.save(user);

        List<String> backupCodes = twoFactorService.generateBackupCodes(user);
        return ResponseEntity.ok(ApiResponse.success(backupCodes));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<AuthResponse>> validate(@RequestBody TwoFactorValidateRequest request) {
        System.out.println("Validating 2FA - TempToken: " + (request.getTempToken() != null));
        
        if (!jwtTokenProvider.validateToken(request.getTempToken())) {
            throw new UnauthorizedException("Token hết hạn");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(request.getTempToken());
        User user = userRepository.findById(userId).orElseThrow();

        boolean isValid;
        if (request.isBackupCode()) {
            System.out.println("Checking backup code: " + request.getCode());
            isValid = twoFactorService.verifyBackupCode(user, request.getCode());
        } else {
            System.out.println("Checking TOTP code: " + request.getCode());
            isValid = twoFactorService.verifyCode(user.getTotpSecret(), Integer.parseInt(request.getCode()));
        }

        if (!isValid) {
            System.out.println("Validation FAILED");
            throw new UnauthorizedException("Mã không đúng");
        }

        System.out.println("Validation SUCCESS");
        UserPrincipal userPrincipal = UserPrincipal.from(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .is2faEnabled(user.getIs2faEnabled())
                        .build())
                .build();

        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<Void>> disable(@AuthenticationPrincipal UserPrincipal currentUser, @RequestBody TwoFactorVerifyRequest request) {
        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        if (twoFactorService.verifyCode(user.getTotpSecret(), request.getCode())) {
            user.setTotpSecret(null);
            user.setIs2faEnabled(false);
            userRepository.save(user);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        throw new UnauthorizedException("Mã không đúng");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TwoFactorSetupResponse {
        private String secret;
        private String qrCodeUri;
    }

    @Data
    public static class TwoFactorVerifyRequest {
        private String secret;
        private int code;
    }

    @Data
    public static class TwoFactorValidateRequest {
        private String tempToken;
        private String code;
        private boolean isBackupCode;
    }
}
