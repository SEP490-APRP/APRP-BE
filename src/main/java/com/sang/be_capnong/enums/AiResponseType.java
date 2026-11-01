package com.sang.be_capnong.enums;

public enum AiResponseType {
    TEXT,
    ITEM_OPTIONS,
    PRODUCT_DETAIL,
    CART_UPDATED,
    CHECKOUT_DRAFT,
    NAVIGATION,
    ERROR,

    // ===== 12 ENHANCED RESPONSE TYPES =====
    ORDER_TRACKING,     // Case 1.1: Thẻ theo dõi đơn hàng & định vị shipper
    RETURN_GUIDANCE,    // Case 1.2: Hướng dẫn đổi trả 24h & tạo đơn khiếu nại
    ORDER_CANCEL,       // Case 1.3: Thẻ xác nhận hủy đơn hàng
    BUDGET_COMBO,       // Case 2.1: Gợi ý combo theo ngân sách
    RECIPE_SUGGESTION,  // Case 2.2: Công thức nấu ăn & nguyên liệu phụ kiện
    MYSTERY_BOX_QA,     // Case 2.3: Thông tin hộp bí ẩn & cảnh báo dị ứng
    VOUCHER_LIST,       // Case 3.1: Danh sách mã giảm giá khả dụng
    SHIPPING_TIPS,      // Case 3.2: Mẹo tối ưu phí giao hàng đa shop
    SAFETY_STANDARDS,   // Case 4.1: Tiêu chuẩn an toàn & VietGAP
    PRESERVATION_TIPS,  // Case 4.2: Mẹo bảo quản rau củ tươi ngon
    HUMAN_HANDOVER,     // Case 5.1: Chuyển tiếp CSKH / Chat P2P với nhà vườn
    ROLE_GUIDANCE,      // Case 5.2: Bảng điều hướng cho Shop & Shipper

    // ===== 8 EXTENDED RESPONSE TYPES =====
    REORDER_RECENT,        // Case 6.1: Mua lại đơn cũ 1-click
    FLASH_RESCUE,          // Case 6.2: Nông sản xả gấp / flash sale
    MEAL_PLAN_BRIDGE,      // Case 7.1: Thực đơn bệnh lý & AI Meal Plan
    SEASONAL_CALENDAR,     // Case 7.2: Lịch mùa vụ nông sản
    GREEN_IMPACT,          // Case 8.1: Tra cứu Dấu chân xanh & kg giải cứu
    PAYMENT_ASSISTANCE,    // Case 9.1: Hỗ trợ thanh toán PayOS / chuyển khoản
    WALLET_INFO,           // Case 9.2: Tra cứu ví CapNong an toàn
    UPDATE_SHIPPING_INFO   // Case 10.1: Đổi thông tin nhận hàng khẩn cấp
}
