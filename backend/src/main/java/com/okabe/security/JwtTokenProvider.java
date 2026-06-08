package com.okabe.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8); // Chuyển secret key thành byte
        // Pad key to minimum 512 bits (64 bytes) for HS512 compatibility
        if (keyBytes.length < 64) { // Nếu key ngắn hơn 64 bytes thì pad thêm
            byte[] padded = new byte[64];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length); // Copy key gốc vào đầu mảng
            keyBytes = padded;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes); // Tạo signing key HMAC-SHA
        this.accessTokenExpiration = accessTokenExpiration; // Thời gian sống của access token
        this.refreshTokenExpiration = refreshTokenExpiration; // Thời gian sống của refresh token
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {
        return buildToken(userPrincipal, accessTokenExpiration, "access"); // Tạo access token
    }

    public String generateRefreshToken(UserPrincipal userPrincipal) {
        return buildToken(userPrincipal, refreshTokenExpiration, "refresh"); // Tạo refresh token
    }

    public String generateTempToken(UserPrincipal userPrincipal) {
        return buildToken(userPrincipal, 300000, "temp"); // 5 minutes // Tạo token tạm thời (5 phút)
    }

    private String buildToken(UserPrincipal userPrincipal, long expiration, String tokenType) {
        Date now = new Date(); // Thời điểm hiện tại
        Date expiryDate = new Date(now.getTime() + expiration); // Thời điểm hết hạn

        return Jwts.builder()
                .subject(String.valueOf(userPrincipal.getId())) // Đặt subject là userId
                .claim("email", userPrincipal.getEmail()) // Claim email
                .claim("username", userPrincipal.getUsername()) // Claim username
                .claim("type", tokenType) // Claim loại token (access/refresh/temp)
                .issuedAt(now) // Thời gian phát hành
                .expiration(expiryDate) // Thời gian hết hạn
                .signWith(signingKey) // Ký token bằng secret key
                .compact(); // Tạo chuỗi JWT
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token); // Giải mã token
        return Long.parseLong(claims.getSubject()); // Lấy userId từ subject
    }

    public String getTokenType(String token) {
        Claims claims = parseToken(token); // Giải mã token
        return claims.get("type", String.class); // Lấy loại token từ claim "type"
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token); // Thử giải mã token
            return true; // Token hợp lệ
        } catch (MalformedJwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage()); // Token sai định dạng
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage()); // Token hết hạn
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage()); // Token không được hỗ trợ
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage()); // Claims rỗng
        }
        return false; // Token không hợp lệ
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey) // Xác thực chữ ký
                .build()
                .parseSignedClaims(token) // Parse JWT thành claims
                .getPayload();
    }
}
