package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ChatbotMessageRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.ChatHistoryResponse;
import com.sang.be_capnong.dto.response.ChatbotMessageResponse;
import com.sang.be_capnong.service.ChatbotService;
import com.sang.be_capnong.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/chatbot", "/chatbot"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ChatbotController {

    ChatbotService chatbotService;

    @PostMapping("/message")
    public ApiResponse<ChatbotMessageResponse> sendMessage(@RequestBody @Valid ChatbotMessageRequest request) {
        Integer userId = SecurityUtils.getCurrentUserIdOrNull();
        log.info("Chatbot message received from userId={}: {}", userId, request.getMessage());
        ChatbotMessageResponse response = chatbotService.handleMessage(request, userId);
        return ApiResponse.<ChatbotMessageResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/history")
    public ApiResponse<List<ChatHistoryResponse>> getChatHistory() {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<ChatHistoryResponse> history = chatbotService.getChatHistory(userId);
        return ApiResponse.<List<ChatHistoryResponse>>builder()
                .result(history)
                .build();
    }

    @DeleteMapping("/history")
    public ApiResponse<Void> clearChatHistory() {
        Integer userId = SecurityUtils.getCurrentUserId();
        chatbotService.clearChatHistory(userId);
        return ApiResponse.<Void>builder()
                .message("Lịch sử trò chuyện đã được xóa thành công.")
                .build();
    }
}
