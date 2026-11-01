package com.sang.be_capnong.enums;

public enum AiIntentType {
    ASK_INFORMATION,
    SEARCH_ITEM,
    BUY_ITEM,
    SELECT_OPTION,
    CHECKOUT,
    ACCOUNT_ACTION,
    UNKNOWN,

    // ===== 12 ENHANCED CASES =====
    TRACK_ORDER,        // Case 1.1: Tra cứu đơn hàng thời gian thực
    RETURN_REQUEST,     // Case 1.2: Hướng dẫn & khiếu nại đổi trả dập úng
    CANCEL_ORDER,       // Case 1.3: Hủy đơn hàng trước khi đóng gói
    BUDGET_COMBO,       // Case 2.1: Mua combo theo ngân sách cố định
    RECIPE_SUGGESTION,  // Case 2.2: Gợi ý món ăn từ rau củ sẵn có
    MYSTERY_BOX_QA,     // Case 2.3: Hộp bí ẩn & cảnh báo dị ứng
    VOUCHER_SUGGESTION, // Case 3.1: Tìm mã giảm giá / freeship
    SHIPPING_TIPS,      // Case 3.2: Mẹo gom đơn cùng shop để tiết kiệm ship
    SAFETY_STANDARDS,   // Case 4.1: Tiêu chuẩn an toàn nông sản xấu mã
    PRESERVATION_TIPS,  // Case 4.2: Mẹo bảo quản rau củ tươi lâu
    HUMAN_HANDOVER,     // Case 5.1: Chuyển tiếp CSKH / gặp người thật
    ROLE_GUIDANCE,      // Case 5.2: Hỗ trợ Nhà vườn (Shop) & Tài xế (Shipper)

    // ===== 8 EXTENDED CASES =====
    REORDER_RECENT,        // Case 6.1: Mua lại đơn hàng cũ 1 cú chạm
    FLASH_RESCUE,          // Case 6.2: Nông sản xả gấp / flash sale cứu trợ
    MEAL_PLAN_BRIDGE,      // Case 7.1: Thực đơn bệnh lý & AI Meal Plan
    SEASONAL_CALENDAR,     // Case 7.2: Lịch mùa vụ nông sản Việt Nam
    GREEN_IMPACT,          // Case 8.1: Tra cứu Dấu chân xanh & kg giải cứu
    PAYMENT_ASSISTANCE,    // Case 9.1: Hỗ trợ thanh toán PayOS / Webhook
    WALLET_INFO,           // Case 9.2: Tra cứu ví CapNong an toàn
    UPDATE_SHIPPING_INFO   // Case 10.1: Đổi thông tin nhận hàng khẩn cấp
}
