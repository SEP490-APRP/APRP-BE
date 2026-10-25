package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ChatMessageRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.ChatMessageResponse;
import com.sang.be_capnong.dto.response.ConversationResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.service.ChatService;
import com.sang.be_capnong.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> getConversations() {
        int currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.<List<ConversationResponse>>builder()
                .code(200)
                .message("Conversations fetched successfully")
                .result(chatService.getUserConversations(currentUserId))
                .build();
    }

    @PostMapping("/conversations/init")
    public ApiResponse<ConversationResponse> initConversation(@RequestBody Map<String, Integer> payload) {
        int currentUserId = SecurityUtils.getCurrentUserId();
        Integer receiverId = payload.get("receiverId");
        if (receiverId == null) {
            throw new IllegalArgumentException("receiverId is required");
        }
        return ApiResponse.<ConversationResponse>builder()
                .code(200)
                .message("Conversation initiated")
                .result(chatService.getOrCreateConversation(currentUserId, receiverId))
                .build();
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<PageResponse<ChatMessageResponse>> getMessages(
            @PathVariable int conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        int currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.<PageResponse<ChatMessageResponse>>builder()
                .code(200)
                .message("Messages fetched successfully")
                .result(chatService.getMessages(conversationId, currentUserId, page, size))
                .build();
    }

    @PostMapping("/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        int currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.<ChatMessageResponse>builder()
                .code(200)
                .message("Message sent successfully")
                .result(chatService.sendMessage(currentUserId, request))
                .build();
    }

    @PatchMapping("/conversations/{conversationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable int conversationId) {
        int currentUserId = SecurityUtils.getCurrentUserId();
        chatService.markAsRead(conversationId, currentUserId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Marked as read")
                .build();
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount() {
        int currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.<Map<String, Long>>builder()
                .code(200)
                .result(Map.of("unreadCount", chatService.getTotalUnreadCount(currentUserId)))
                .build();
    }

    @DeleteMapping("/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(@PathVariable int messageId) {
        int currentUserId = SecurityUtils.getCurrentUserId();
        chatService.deleteMessage(messageId, currentUserId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Message deleted successfully")
                .build();
    }
}
