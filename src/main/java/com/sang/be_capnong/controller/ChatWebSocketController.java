package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ChatMessageRequest;
import com.sang.be_capnong.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Client gửi tin nhắn real-time tới: /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequest request, Principal principal) {
        if (principal == null) {
            log.warn("[WS Chat] Unauthorized send attempt");
            return;
        }

        try {
            int senderId = Integer.parseInt(principal.getName());
            log.info("[WS Chat] Received message from userId={} to userId={}", senderId, request.getReceiverId());
            chatService.sendMessage(senderId, request);
        } catch (Exception e) {
            log.error("[WS Chat] Failed to process message: {}", e.getMessage(), e);
        }
    }

    /**
     * Client gửi tín hiệu đang soạn tin nhắn tới: /app/chat.typing
     * payload: { "receiverId": 123, "isTyping": true }
     */
    @MessageMapping("/chat.typing")
    public void sendTyping(Map<String, Object> payload, Principal principal) {
        if (principal == null) return;

        try {
            int senderId = Integer.parseInt(principal.getName());
            Object receiverObj = payload.get("receiverId");
            if (receiverObj == null) return;

            int receiverId = Integer.parseInt(receiverObj.toString());
            boolean isTyping = Boolean.TRUE.equals(payload.get("isTyping"));

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(receiverId),
                    "/queue/typing",
                    Map.of(
                            "senderId", senderId,
                            "isTyping", isTyping
                    )
            );
        } catch (Exception e) {
            log.warn("[WS Chat] Error in typing indicator: {}", e.getMessage());
        }
    }

    /**
     * Client gửi tín hiệu đã đọc hội thoại tới: /app/chat.read
     * payload: { "conversationId": 456 }
     */
    @MessageMapping("/chat.read")
    public void markAsRead(Map<String, Object> payload, Principal principal) {
        if (principal == null) return;

        try {
            int userId = Integer.parseInt(principal.getName());
            Object convIdObj = payload.get("conversationId");
            if (convIdObj == null) return;

            int conversationId = Integer.parseInt(convIdObj.toString());
            chatService.markAsRead(conversationId, userId);
        } catch (Exception e) {
            log.warn("[WS Chat] Error in markAsRead: {}", e.getMessage());
        }
    }
}
