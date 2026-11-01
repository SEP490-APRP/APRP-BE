package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.AiDetectedIntent;
import com.sang.be_capnong.entity.ChatBot;

import java.util.List;

public interface ChatbotAiClient {
    AiDetectedIntent detectIntent(String userMessage);
    String generateAnswer(String userMessage, String ragContext, String catalogContext, List<ChatBot> history);
}
