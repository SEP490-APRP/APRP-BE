package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.AiDetectedIntent;
import com.sang.be_capnong.dto.request.ChatbotMessageRequest;
import com.sang.be_capnong.dto.response.ChatHistoryResponse;
import com.sang.be_capnong.dto.response.ChatbotMessageResponse;
import com.sang.be_capnong.entity.ChatBot;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.AiItemType;
import com.sang.be_capnong.enums.AiResponseType;
import com.sang.be_capnong.repository.ChatBotRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.ChatbotActionService;
import com.sang.be_capnong.service.ChatbotAiClient;
import com.sang.be_capnong.service.ChatbotService;
import com.sang.be_capnong.service.RagKnowledgeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    ChatBotRepository chatBotRepository;
    UserRepository userRepository;
    RagKnowledgeService ragKnowledgeService;
    ChatbotAiClient chatbotAiClient;
    ChatbotActionService chatbotActionService;

    @Override
    @Transactional
    public ChatbotMessageResponse handleMessage(ChatbotMessageRequest request, Integer userId) {
        User user = (userId != null) ? userRepository.findById(userId).orElse(null) : null;

        // 1. Direct Option Selection
        if (request.getSelectedOptionId() != null && request.getSelectedOptionType() != null) {
            ChatbotMessageResponse response = chatbotActionService.selectItemOption(
                    request.getSelectedOptionId(),
                    request.getSelectedOptionType(),
                    request.getSelectedQuantity(),
                    userId
            );

            if (user != null && response != null && response.getMessage() != null) {
                saveChatRecord(user, "assistant", response.getMessage());
            }
            return response;
        }

        String message = (request.getMessage() != null) ? request.getMessage().trim() : "";
        if (message.isEmpty()) {
            return ChatbotMessageResponse.text("Chào bạn, mình là CapNong Copilot! Mình có thể giúp gì cho bạn hôm nay?");
        }

        // 2. Persist user message to chat history
        if (user != null) {
            saveChatRecord(user, "user", message);
        }

        // 3. Build RAG & catalog contexts
        String ragContext = ragKnowledgeService.buildKnowledgeContext(message);
        String catalogContext = ragKnowledgeService.buildCatalogContext();

        // 4. Fetch recent conversation messages
        List<ChatBot> historyEntities = Collections.emptyList();
        if (user != null) {
            List<ChatBot> recentTop = chatBotRepository.findTop10ByUser_IdOrderByCreatedAtDesc(user.getId());
            if (recentTop != null && !recentTop.isEmpty()) {
                historyEntities = new ArrayList<>(recentTop);
                Collections.reverse(historyEntities);
            }
        }

        // 5. Detect intent via AI client
        AiDetectedIntent detectedIntent = chatbotAiClient.detectIntent(message);
        log.info("Detected AI intent: {}", detectedIntent.getIntent());

        ChatbotMessageResponse response;

        // 6. Dispatch across all 12 enhanced cases
        switch (detectedIntent.getIntent()) {
            // Case 1.1: Tra cứu đơn hàng
            case TRACK_ORDER:
                response = chatbotActionService.trackOrder(detectedIntent.getOrderId(), userId);
                break;

            // Case 1.2: Hướng dẫn đổi trả dập úng 24h
            case RETURN_REQUEST:
                response = chatbotActionService.handleReturnGuidance(userId);
                break;

            // Case 1.3: Hủy đơn hàng
            case CANCEL_ORDER:
                response = chatbotActionService.cancelOrder(detectedIntent.getOrderId(), userId);
                break;

            // Case 2.1: Combo theo ngân sách
            case BUDGET_COMBO:
                response = chatbotActionService.buildBudgetCombo(detectedIntent.getBudget(), message, userId);
                break;

            // Case 2.2: Gợi ý công thức món ăn
            case RECIPE_SUGGESTION:
                response = chatbotActionService.suggestRecipe(detectedIntent.getIngredientsQuery(), userId);
                break;

            // Case 2.3: Hộp bí ẩn & cảnh báo dị ứng
            case MYSTERY_BOX_QA:
                response = chatbotActionService.getMysteryBoxQa(message, userId);
                break;

            // Case 3.1: Voucher & Freeship
            case VOUCHER_SUGGESTION:
                response = chatbotActionService.getAvailableVouchers(detectedIntent.getKeyword(), message, userId);
                break;

            // Case 3.2: Tối ưu phí ship gom đơn
            case SHIPPING_TIPS:
                response = chatbotActionService.getShippingTips(userId);
                break;

            // Case 4.1: Tiêu chuẩn an toàn nông sản xấu mã
            case SAFETY_STANDARDS:
                String safetyAnswer = chatbotAiClient.generateAnswer(message, ragContext, catalogContext, historyEntities);
                response = ChatbotMessageResponse.builder()
                        .type(AiResponseType.SAFETY_STANDARDS)
                        .message(safetyAnswer != null && !safetyAnswer.isBlank()
                                ? safetyAnswer
                                : "100% nông sản trên CapNong đạt chuẩn VietGAP/GlobalGAP hoặc canh tác hữu cơ tự nhiên. 'Xấu mã' chỉ là ngoại quan (cong queo, không đều màu) chứ hoàn toàn tươi mới, an toàn và bổ dưỡng tương đương loại 1!")
                        .timestamp(LocalDateTime.now())
                        .build();
                break;

            // Case 4.2: Mẹo bảo quản rau củ tươi lâu
            case PRESERVATION_TIPS:
                response = chatbotActionService.getPreservationTips(message);
                break;

            // Case 5.1: Chuyển tiếp CSKH / gặp người thật
            case HUMAN_HANDOVER:
                response = chatbotActionService.handleHumanHandover();
                break;

            // Case 5.2: Hỗ trợ Nhà vườn & Tài xế theo vai trò
            case ROLE_GUIDANCE:
                response = chatbotActionService.getRoleGuidance(message, userId);
                break;

            // Case 6.1: Đặt lại đơn hàng cũ 1 chạm
            case REORDER_RECENT:
                response = chatbotActionService.reorderRecent(detectedIntent.getOrderId(), userId);
                break;

            // Case 6.2: Flash rescue / nông sản giải cứu 24h
            case FLASH_RESCUE:
                response = chatbotActionService.getFlashRescueItems();
                break;

            // Case 7.1: Gợi ý thực đơn cá nhân hóa (Bệnh lý / Eat clean)
            case MEAL_PLAN_BRIDGE:
                response = chatbotActionService.bridgeMealPlan(message, userId);
                break;

            // Case 7.2: Lịch mùa vụ nông sản Việt Nam
            case SEASONAL_CALENDAR:
                response = chatbotActionService.getSeasonalCalendar(detectedIntent.getMonth(), message);
                break;

            // Case 8.1: Dấu chân xanh & Tác động giải cứu nông sản
            case GREEN_IMPACT:
                response = chatbotActionService.getGreenImpact(userId);
                break;

            // Case 9.1: Hỗ trợ thanh toán PayOS / Ngân hàng
            case PAYMENT_ASSISTANCE:
                response = chatbotActionService.assistPayment(detectedIntent.getOrderId(), userId);
                break;

            // Case 9.2: Hướng dẫn Ví & Điểm tích lũy (Wallet Info)
            case WALLET_INFO:
                response = chatbotActionService.getWalletInfo(userId);
                break;

            // Case 10.1: Cập nhật thông tin nhận hàng khẩn cấp
            case UPDATE_SHIPPING_INFO:
                response = chatbotActionService.handleUpdateShippingInfo(detectedIntent.getOrderId(), message, userId);
                break;

            // Thao tác mua hàng & giỏ hàng
            case BUY_ITEM:
                response = chatbotActionService.buyItems(detectedIntent, userId);
                break;

            case SEARCH_ITEM:
                String rawKw = detectedIntent.getKeyword();
                if (rawKw == null || rawKw.isBlank()) {
                    rawKw = message;
                }
                String cleanedKw = GeminiChatbotAiClient.cleanSearchKeyword(rawKw);
                if (cleanedKw.isBlank()) {
                    cleanedKw = GeminiChatbotAiClient.cleanSearchKeyword(message);
                }

                boolean isComboSearch = (rawKw != null && rawKw.toLowerCase().contains("combo"))
                        || (message != null && message.toLowerCase().contains("combo"))
                        || detectedIntent.getItemType() == AiItemType.BUILD_COMBO;

                AiItemType searchType = isComboSearch ? AiItemType.BUILD_COMBO : (detectedIntent.getItemType() != null ? detectedIntent.getItemType() : AiItemType.PRODUCT);

                List<ChatbotMessageResponse.ItemOptionDto> items = chatbotActionService.searchItems(
                        cleanedKw,
                        searchType
                );

                // Nếu tìm theo combo mà không có kết quả với đúng tên, thử tìm mở rộng
                if (items.isEmpty() && isComboSearch) {
                    String subKw = cleanedKw.toLowerCase().replace("combo", "").trim();
                    if (!subKw.isEmpty()) {
                        items = chatbotActionService.searchItems(subKw, AiItemType.BUILD_COMBO);
                    }
                    if (items.isEmpty() && !subKw.isEmpty()) {
                        items = chatbotActionService.searchItems(subKw, AiItemType.PRODUCT);
                    }
                }

                if (items.isEmpty()) {
                    // Không tìm thấy sản phẩm khớp -> Lấy các combo hoặc nông sản có sẵn trên sàn để gợi ý cho khách
                    List<ChatbotMessageResponse.ItemOptionDto> fallbackItems = isComboSearch
                            ? chatbotActionService.searchItems(null, AiItemType.BUILD_COMBO)
                            : Collections.emptyList();

                    if (fallbackItems.isEmpty()) {
                        fallbackItems = chatbotActionService.searchItems(null, AiItemType.PRODUCT);
                    }

                    if (!fallbackItems.isEmpty()) {
                        String msg = (!cleanedKw.isEmpty())
                                ? "Hiện tại sàn CapNong chưa có sẵn " + (isComboSearch ? "gói combo '" : "nông sản '") + cleanedKw + "'. Bạn có thể tham khảo các " + (isComboSearch ? "combo và " : "") + "nông sản tươi ngon khác đang có sẵn trên sàn hôm nay nhé:"
                                : "Hiện tại chưa tìm thấy sản phẩm theo yêu cầu. Gợi ý bạn một số " + (isComboSearch ? "combo và " : "") + "nông sản tươi ngon hôm nay:";
                        response = ChatbotMessageResponse.builder()
                                .type(AiResponseType.ITEM_OPTIONS)
                                .message(msg)
                                .itemOptions(fallbackItems.stream().limit(4).collect(Collectors.toList()))
                                .timestamp(LocalDateTime.now())
                                .build();
                    } else {
                        response = ChatbotMessageResponse.text("Hiện tại sàn CapNong chưa có sản phẩm '" + cleanedKw + "'. Bạn hãy tham khảo các sản phẩm khác trên trang chủ nhé!");
                    }
                } else {
                    String msg = (!cleanedKw.isEmpty())
                            ? "CapNong gửi bạn danh sách " + (isComboSearch ? "gói combo" : "nông sản") + " tìm thấy cho '" + cleanedKw + "':"
                            : "CapNong gửi bạn danh sách " + (isComboSearch ? "gói combo" : "nông sản") + " đang có sẵn:";
                    response = ChatbotMessageResponse.builder()
                            .type(AiResponseType.ITEM_OPTIONS)
                            .message(msg)
                            .itemOptions(items)
                            .timestamp(LocalDateTime.now())
                            .build();
                }
                break;

            case CHECKOUT:
                response = chatbotActionService.createCheckoutDraft(userId);
                break;

            case ACCOUNT_ACTION:
                response = chatbotActionService.handleAccountAction(detectedIntent.getAccountAction(), message, userId);
                break;

            case ASK_INFORMATION:
            case UNKNOWN:
            default:
                String answer = chatbotAiClient.generateAnswer(message, ragContext, catalogContext, historyEntities);
                String reply = (answer != null && !answer.isBlank())
                        ? answer
                        : "CapNong Copilot luôn sẵn lòng hỗ trợ bạn! Hãy cho mình biết bạn cần tìm sản phẩm gì hoặc cần giúp đỡ thông tin nào nhé.";
                response = ChatbotMessageResponse.text(reply);
                break;
        }

        // 7. Persist assistant reply to chat history
        if (user != null && response != null && response.getMessage() != null) {
            saveChatRecord(user, "assistant", response.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> getChatHistory(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<ChatBot> list = chatBotRepository.findByUser_IdOrderByCreatedAtAsc(userId);
        return list.stream()
                .map(c -> new ChatHistoryResponse(
                        c.getId(),
                        c.getRole(),
                        c.getContent(),
                        c.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clearChatHistory(Integer userId) {
        if (userId != null) {
            chatBotRepository.deleteByUser_Id(userId);
        }
    }

    private void saveChatRecord(User user, String role, String content) {
        try {
            ChatBot chat = ChatBot.builder()
                    .user(user)
                    .role(role)
                    .content(content)
                    .createdAt(LocalDateTime.now())
                    .build();
            chatBotRepository.save(chat);
        } catch (Exception e) {
            log.error("Failed to save chatbot message record: {}", e.getMessage());
        }
    }
}
