package com.okabe.security;

import com.okabe.entity.User;
import com.okabe.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractTokenFromRequest(request); // Lấy JWT từ request
            log.debug("Extracted JWT: {}", jwt != null ? "present" : "null");
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) { // Nếu token tồn tại và hợp lệ
                String tokenType = jwtTokenProvider.getTokenType(jwt); // Lấy loại token (access/refresh/temp)
                log.debug("Token type: {}", tokenType);

                if ("access".equals(tokenType)) { // Chỉ xác thực nếu là access token
                    Long userId = jwtTokenProvider.getUserIdFromToken(jwt); // Lấy userId từ token
                    log.debug("Authenticated userId: {}", userId);

                    User user = userRepository.findById(userId).orElse(null); // Tìm user trong DB
                    if (user != null && user.getIsActive()) { // Nếu user tồn tại và còn hoạt động
                        UserDetails userDetails = UserPrincipal.from(user); // Tạo UserPrincipal

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities()); // Tạo đối tượng xác thực
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)); // Gán thông tin chi tiết từ request

                        SecurityContextHolder.getContext().setAuthentication(authentication); // Đặt vào SecurityContext
                        log.debug("Security context set for user: {}", user.getEmail());
                    } else {
                        log.warn("User not found or inactive for id: {}", userId);
                    }
                } else {
                    log.warn("Invalid token type for authentication: {}", tokenType);
                }
            } else if (StringUtils.hasText(jwt)) { // Token có tồn tại nhưng không hợp lệ
                log.warn("JWT token validation failed");
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response); // Tiếp tục chuỗi filter
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization"); // Lấy header Authorization
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) { // Nếu header có dạng "Bearer <token>"
            return bearerToken.substring(7); // Trả về token (loại bỏ "Bearer ")
        }
        return null; // Không tìm thấy token
    }

}
