package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sang.be_capnong.enums.AiItemType;
import com.sang.be_capnong.enums.AiResponseType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatbotMessageResponse {

    AiResponseType type;
    String message;
    List<ItemOptionDto> itemOptions;
    ItemOptionDto selectedProduct;
    CartPreviewDto cart;
    CheckoutDraftDto checkout;
    NavigationDto navigation;

    // ===== 12 ENHANCED PAYLOADS =====
    OrderTrackingDto orderTracking;
    ReturnGuidanceDto returnGuidance;
    OrderCancelDto orderCancel;
    BudgetComboDto budgetCombo;
    RecipeSuggestionDto recipe;
    MysteryBoxQaDto mysteryBoxQa;
    List<VoucherItemDto> vouchers;
    ShippingTipDto shippingTips;
    List<PreservationTipDto> preservationTips;
    HumanHandoverDto humanHandover;
    RoleGuidanceDto roleGuidance;

    // ===== 8 EXTENDED PAYLOADS =====
    ReorderDto reorder;
    FlashRescueDto flashRescue;
    MealPlanBridgeDto mealPlanBridge;
    SeasonalCalendarDto seasonalCalendar;
    GreenImpactDto greenImpact;
    PaymentAssistanceDto paymentAssistance;
    WalletInfoDto walletInfo;
    UpdateShippingInfoDto updateShippingInfo;

    LocalDateTime timestamp;

    public static ChatbotMessageResponse text(String message) {
        return ChatbotMessageResponse.builder()
                .type(AiResponseType.TEXT)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ChatbotMessageResponse error(String message) {
        return ChatbotMessageResponse.builder()
                .type(AiResponseType.ERROR)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ItemOptionDto {
        AiItemType itemType;
        Integer itemId;
        String name;
        String imageUrl;
        BigDecimal price;
        BigDecimal salePrice;
        Integer discountPercent;
        String unit;
        Integer availableQuantity;
        String description;
        Boolean isOutOfStock;
        Integer orderQuantity;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CartItemDto {
        AiItemType itemType;
        Integer itemId;
        String name;
        String imageUrl;
        Integer quantity;
        String unit;
        BigDecimal unitPrice;
        BigDecimal totalPrice;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CartPreviewDto {
        List<CartItemDto> items;
        BigDecimal totalAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CheckoutDraftDto {
        String checkoutUrl;
        BigDecimal totalAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NavigationDto {
        String url;
        String label;
    }

    // ===== 12 ENHANCED DTOS =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderTrackingDto {
        Integer orderId;
        String orderStatus;
        String statusDescription;
        String shopName;
        BigDecimal totalAmount;
        String recipientName;
        String recipientPhone;
        String shippingAddress;
        String shipperName;
        String shipperPhone;
        String trackingUrl;
        LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReturnGuidanceDto {
        /** Đơn gần nhất còn trong hạn trả hàng; null nếu không có đơn nào đủ điều kiện. */
        Integer orderId;
        String policyTitle;
        String policyContent;
        String actionUrl;
        String actionLabel;
        /** Hạn chót gửi yêu cầu cho đơn orderId. */
        LocalDateTime returnDeadline;
        /** Yêu cầu trả hàng gần nhất của khách, lấy từ /returns. */
        LatestReturnDto latestReturn;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LatestReturnDto {
        Integer returnId;
        Integer orderId;
        String productName;
        /** Mã ReturnStatus của BE. */
        String status;
        String statusLabel;
        BigDecimal refundAmount;
        boolean active;
        LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderCancelDto {
        Integer orderId;
        String orderStatus;
        boolean canCancel;
        String message;
        BigDecimal refundAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BudgetComboDto {
        BigDecimal targetBudget;
        BigDecimal actualTotal;
        String title;
        String description;
        List<ItemOptionDto> items;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecipeSuggestionDto {
        String dishName;
        String cookTime;
        String description;
        List<String> userIngredients;
        List<ItemOptionDto> missingIngredients;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MysteryBoxQaDto {
        String seasonalDescription;
        List<String> userAllergies;
        ItemOptionDto boxItem;
        String note;
        Boolean isAllergyRisk;
        String riskAllergen;
        String safetyRecommendation;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VoucherItemDto {
        Integer voucherId;
        String code;
        String title;
        BigDecimal discountValue;
        BigDecimal minOrderValue;
        BigDecimal maxDiscount;
        String expiryDate;
        String voucherType;
        String voucherScope;
        Integer shopId;
        String shopName;
        Boolean isCollected;
        Boolean isOutOfStock;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShippingTipDto {
        boolean hasMultiShops;
        int shopCount;
        List<String> shopNames;
        String tipMessage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PreservationTipDto {
        String category;
        String tips;
        String storageType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HumanHandoverDto {
        String hotline;
        String zaloUrl;
        String email;
        boolean triggerP2pChat;
        String note;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoleGuidanceDto {
        String role;
        String title;
        String guidance;
        List<NavigationDto> quickLinks;
    }

    // ===== 8 EXTENDED DTOS =====

    // Case 6.1: Reorder DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReorderDto {
        Integer orderId;
        String orderDate;
        String shopName;
        String orderStatus;
        BigDecimal totalAmount;
        List<ItemOptionDto> items;
        boolean canReorder;
        Integer availableItemsCount;
        Integer outOfStockItemsCount;
        String stockWarning;
    }

    // Case 6.2: Flash Rescue DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FlashRescueDto {
        String title;
        String description;
        Integer discountRate;
        List<ItemOptionDto> items;
    }

    // Case 7.1: Meal Plan Bridge DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MealPlanBridgeDto {
        String conditionName;
        String healthAdvice;
        List<String> sampleMeals;
        List<String> goodProduce;
        List<String> avoidProduce;
        List<ItemOptionDto> recommendedProducts;
        List<String> suggestedConditions;
        String ctaMessage;
        String planUrl;
        String actionLabel;

        @com.fasterxml.jackson.annotation.JsonProperty("topic")
        public String getTopic() { return conditionName; }

        @com.fasterxml.jackson.annotation.JsonProperty("suggestionSummary")
        public String getSuggestionSummary() { return healthAdvice; }

        @com.fasterxml.jackson.annotation.JsonProperty("sampleDishes")
        public List<String> getSampleDishes() { return sampleMeals; }

        @com.fasterxml.jackson.annotation.JsonProperty("recommendedProduce")
        public List<ItemOptionDto> getRecommendedProduce() { return recommendedProducts; }

        @com.fasterxml.jackson.annotation.JsonProperty("ctaUrl")
        public String getCtaUrl() { return planUrl; }

        @com.fasterxml.jackson.annotation.JsonProperty("ctaLabel")
        public String getCtaLabel() { return actionLabel; }
    }

    // Case 7.2: Seasonal Calendar DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SeasonalCalendarDto {
        String currentMonth;
        List<String> inSeasonFruits;
        List<String> inSeasonVeggies;
        List<String> inSeasonVegetables;
        List<String> upcomingProduce;
        String highlights;
        String healthBenefitNote;
        Boolean isPastMonth;
        Boolean isFutureMonth;
        String note;
    }

    // Case 8.1: Green Impact DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GreenImpactDto {
        BigDecimal totalRescuedWeight;
        BigDecimal totalRescuedKg;
        BigDecimal co2SavedKg;
        BigDecimal totalCo2SavedKg;
        BigDecimal loyaltyPoints;
        Integer treesEquivalent;
        String badgeLevel;
        String ecoRank;
        String impactMessage;
        String shareMessage;
    }

    // Case 9.1: Payment Assistance DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentAssistanceDto {
        Integer orderId;
        String paymentMethod;
        String paymentStatus;
        String guidance;
        String supportHotline;
    }

    // Case 9.2: Wallet Info DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WalletInfoDto {
        String walletStatus;
        /** BUYER | SHOP | SHIPPER */
        String walletType;
        /** Số dư rút được. Ví Shipper có thể âm khi đang giữ tiền mặt COD chưa nộp về sàn. */
        BigDecimal balance;
        /** Tiền tạm giữ, nhận khi đơn hoàn tất. */
        BigDecimal frozenBalance;
        BigDecimal totalWithdrawn;
        List<WalletTransactionDto> recentTransactions;
        BigDecimal loyaltyPoints;
        String linkedBankName;
        String linkedBankAccount;
        String message;
        String navigationUrl;
        String navigationLabel;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WalletTransactionDto {
        String description;
        /** Dương là cộng, âm là trừ. */
        BigDecimal amount;
        /** true: bút toán tạm giữ, chưa đổi số dư rút được. */
        boolean hold;
        LocalDateTime createdAt;
    }

    // Case 10.1: Update Shipping Info DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdateShippingInfoDto {
        Integer orderId;
        String currentAddress;
        String currentPhone;
        boolean canUpdate;
        String updateType; // "PHONE" | "ADDRESS"
        String shipperName;
        String shipperPhone;
        String orderStatus;
        String guidance;
        String note;
    }
}
