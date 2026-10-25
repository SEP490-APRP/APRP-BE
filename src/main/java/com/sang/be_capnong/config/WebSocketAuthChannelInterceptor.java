package com.sang.be_capnong.config;

import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final CustomJwtDecoder customJwtDecoder;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[WS Auth] Connection rejected: Missing or invalid Authorization header");
                throw new MessageDeliveryException("Unauthorized: Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            try {
                Jwt jwt = customJwtDecoder.decode(token);
                String subject = jwt.getSubject();

                int userId = Integer.parseInt(subject);
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new MessageDeliveryException("Unauthorized: User not found"));

                String role = "ROLE_" + (user.getRole() != null ? user.getRole().getName() : "USER");
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                subject,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                accessor.setUser(authentication);
                log.info("WebSocket CONNECT authenticated | userId={} | role={}", userId, role);
            } catch (MessageDeliveryException e) {
                throw e;
            } catch (Exception e) {
                log.warn("[WS Auth] Connection rejected: Token verification failed: {}", e.getMessage());
                throw new MessageDeliveryException("Unauthorized: Token verification failed");
            }
        }

        return message;
    }
}
