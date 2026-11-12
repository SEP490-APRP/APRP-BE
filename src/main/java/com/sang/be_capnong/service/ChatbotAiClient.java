package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.AiDetectedIntent;
import com.sang.be_capnong.entity.ChatBot;

import java.util.List;

public interface ChatbotAiClient {
    AiDetectedIntent detectIntent(String userMessage);
    String generateAnswer(String userMessage, String ragContext, String catalogContext, List<ChatBot> history);

    /** Gợi ý món từ nguyên liệu của khách, tránh các chất gây dị ứng. Trả null khi AI không dùng được. */
    RecipeIdea suggestRecipe(String ingredients, List<String> allergens);

    record RecipeIdea(String dishName, String cookTime, String description, List<String> extraIngredients) {
    }
}
