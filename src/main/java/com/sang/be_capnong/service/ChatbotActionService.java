package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.AiDetectedIntent;
import com.sang.be_capnong.dto.response.ChatbotMessageResponse;
import com.sang.be_capnong.enums.AiAccountActionType;
import com.sang.be_capnong.enums.AiItemType;

import java.math.BigDecimal;
import java.util.List;

public interface ChatbotActionService {

    List<ChatbotMessageResponse.ItemOptionDto> searchItems(String keyword, AiItemType preferredType);

    ChatbotMessageResponse buyItems(AiDetectedIntent intent, Integer userId);

    ChatbotMessageResponse selectItemOption(Integer itemId, AiItemType itemType, Integer quantity, Integer userId);

    ChatbotMessageResponse createCheckoutDraft(Integer userId);

    ChatbotMessageResponse handleAccountAction(AiAccountActionType actionType, Integer userId);

    ChatbotMessageResponse handleAccountAction(AiAccountActionType actionType, String userMessage, Integer userId);

    // ===== 12 ENHANCED ACTION METHODS =====
    ChatbotMessageResponse trackOrder(Integer orderId, Integer userId);

    ChatbotMessageResponse handleReturnGuidance(Integer userId);

    ChatbotMessageResponse cancelOrder(Integer orderId, Integer userId);

    ChatbotMessageResponse buildBudgetCombo(BigDecimal budget, Integer userId);

    ChatbotMessageResponse buildBudgetCombo(BigDecimal budget, String userMessage, Integer userId);

    ChatbotMessageResponse suggestRecipe(String userIngredients, Integer userId);

    ChatbotMessageResponse getMysteryBoxQa(Integer userId);

    ChatbotMessageResponse getMysteryBoxQa(String userMessage, Integer userId);

    ChatbotMessageResponse getAvailableVouchers(String filterKeyword, String rawMessage, Integer userId);

    ChatbotMessageResponse getShippingTips(Integer userId);

    ChatbotMessageResponse getPreservationTips(String query);

    ChatbotMessageResponse handleHumanHandover();

    ChatbotMessageResponse getRoleGuidance(Integer userId);

    ChatbotMessageResponse getRoleGuidance(String userMessage, Integer userId);

    // ===== 8 EXTENDED ACTION METHODS =====
    ChatbotMessageResponse reorderRecent(Integer orderId, Integer userId);

    ChatbotMessageResponse getFlashRescueItems();

    ChatbotMessageResponse bridgeMealPlan(String conditionQuery, Integer userId);

    ChatbotMessageResponse getSeasonalCalendar();

    ChatbotMessageResponse getSeasonalCalendar(Integer targetMonth, String userMessage);

    ChatbotMessageResponse getGreenImpact(Integer userId);

    ChatbotMessageResponse assistPayment(Integer orderId, Integer userId);

    ChatbotMessageResponse getWalletInfo(Integer userId);

    ChatbotMessageResponse handleUpdateShippingInfo(Integer orderId, Integer userId);

    ChatbotMessageResponse handleUpdateShippingInfo(Integer orderId, String userMessage, Integer userId);
}
