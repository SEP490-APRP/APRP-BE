package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ChatbotMessageRequest;
import com.sang.be_capnong.dto.response.ChatHistoryResponse;
import com.sang.be_capnong.dto.response.ChatbotMessageResponse;

import java.util.List;

public interface ChatbotService {

    ChatbotMessageResponse handleMessage(ChatbotMessageRequest request, Integer userId);

    List<ChatHistoryResponse> getChatHistory(Integer userId);

    void clearChatHistory(Integer userId);
}
