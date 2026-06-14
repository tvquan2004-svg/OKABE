package com.okabe.config;

import com.okabe.security.CustomUserDetailsService;
import com.okabe.security.JwtTokenProvider;
import com.okabe.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // Bật broker đơn giản cho các topic và queue
        config.setApplicationDestinationPrefixes("/app"); // Tiền tố cho các message gửi từ client đến server
        config.setUserDestinationPrefix("/user"); // Tiền tố cho các message gửi đến user cụ thể
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // Endpoint WebSocket
                .setAllowedOriginPatterns("*") // Cho phép mọi nguồn kết nối
                .withSockJS(); // Hỗ trợ fallback SockJS cho trình duyệt không hỗ trợ WebSocket
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class); // Lấy accessor để đọc header STOMP

                if (StompCommand.CONNECT.equals(accessor.getCommand())) { // Kiểm tra nếu là lệnh CONNECT
                    String token = accessor.getFirstNativeHeader("Authorization"); // Lấy token từ header Authorization
                    log.debug("WebSocket connect attempt with token: {}", token != null ? "present" : "absent");

                    if (StringUtils.hasText(token) && token.startsWith("Bearer ")) { // Nếu token hợp lệ và có dạng Bearer
                        token = token.substring(7); // Loại bỏ tiền tố "Bearer "
                        if (tokenProvider.validateToken(token)) { // Xác thực token JWT
                            Long userId = tokenProvider.getUserIdFromToken(token); // Lấy userId từ token
                            UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserById(userId); // Tải thông tin user
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    userPrincipal, null, userPrincipal.getAuthorities()); // Tạo đối tượng xác thực
                            
                            accessor.setUser(auth); // Gán user cho session WebSocket
                            log.debug("WebSocket authenticated user: {}", userPrincipal.getUsername());
                        }
                    }
                }
                return message; // Tiếp tục xử lý message
            }
        });
    }
}
