package com.sang.be_capnong.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999,"Uncategorized Error", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXISTED(1001, "User already exists", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1002, "Role not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1003, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1004, "Unauthorized", HttpStatus.FORBIDDEN),
    INVALID_KEY(1005, "Invalid key", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(10010, "User not existed",HttpStatus.NOT_FOUND),
    MISS_ROLE(10011, "Missing required role", HttpStatus.FORBIDDEN),
//   Create user errors
    EMAIL_INVALID(1006, "Email is invalid", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED(1009, "Email is required", HttpStatus.BAD_REQUEST),
    EMAIL_TOO_LONG(1010, "Email must not exceed 100 characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1007, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1008, "Password is required", HttpStatus.BAD_REQUEST),
    FULL_NAME_REQUIRED(1011, "Full name is required", HttpStatus.BAD_REQUEST),
    PHONE_NUMBER_REQUIRED(1012, "Phone number is required", HttpStatus.BAD_REQUEST),
    PHONE_NUMBER_INVALID(1013, "Phone number format is invalid", HttpStatus.BAD_REQUEST),
    ADDRESS_REQUIRED(1014, "Address is required", HttpStatus.BAD_REQUEST),
    SHOP_NAME_REQUIRED(1015, "Shop name is required", HttpStatus.BAD_REQUEST),
    VEHICLE_NUMBER_REQUIRED(1016, "Vehicle number is required", HttpStatus.BAD_REQUEST),
    PHONE_EXISTED(1017, "Phone number is already registered", HttpStatus.BAD_REQUEST),
    OTP_REQUIRED(1018, "OTP is required", HttpStatus.BAD_REQUEST),
    OTP_INVALID(1019, "OTP is invalid", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1020, "OTP has expired", HttpStatus.BAD_REQUEST),
    OTP_NOT_FOUND(1021, "No OTP found for this email", HttpStatus.NOT_FOUND),
    USER_NOT_ACTIVE(1022, "Account is not verified yet. Please verify your OTP", HttpStatus.FORBIDDEN),
    USER_ALREADY_ACTIVE(1023, "Account is already verified and active", HttpStatus.BAD_REQUEST),
    SEND_MAIL_FAILED(1024, "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_PENDING_APPROVAL(1025, "Account is pending admin approval. Please wait for approval before accessing the platform", HttpStatus.FORBIDDEN),
    USER_LOCKED(1026, "Account has been locked. Please contact support", HttpStatus.FORBIDDEN),


    // Category errors
    CATEGORY_NOT_FOUND(2001, "Category not found", HttpStatus.NOT_FOUND),
    CATEGORY_EXISTED(2002, "Category name already exists", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_REQUIRED(2003, "Category name is required", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_TOO_LONG(2004, "Category name must not exceed 100 characters", HttpStatus.BAD_REQUEST),
    CATEGORY_DESCRIPTION_TOO_LONG(2005, "Category description must not exceed 500 characters", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_PRODUCTS(2006, "Cannot delete category that contains products", HttpStatus.BAD_REQUEST),

    // Product errors
    PRODUCT_NOT_FOUND(3001, "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_NAME_REQUIRED(3002, "Product name is required", HttpStatus.BAD_REQUEST),
    PRODUCT_NAME_TOO_LONG(3003, "Product name must not exceed 255 characters", HttpStatus.BAD_REQUEST),
    PRODUCT_PRICE_REQUIRED(3004, "Product price is required", HttpStatus.BAD_REQUEST),
    PRODUCT_PRICE_INVALID(3005, "Product price must be greater than or equal to 0", HttpStatus.BAD_REQUEST),
    PRODUCT_STOCK_REQUIRED(3006, "Stock quantity is required", HttpStatus.BAD_REQUEST),
    PRODUCT_STOCK_INVALID(3007, "Stock quantity must be greater than or equal to 0", HttpStatus.BAD_REQUEST),
    PRODUCT_EXPIRY_REQUIRED(3008, "Expiry date is required", HttpStatus.BAD_REQUEST),
    PRODUCT_EXPIRY_INVALID(3009, "Expiry date must not be in the past", HttpStatus.BAD_REQUEST),
    PRODUCT_CATEGORY_REQUIRED(3010, "Category ID is required", HttpStatus.BAD_REQUEST),
    PRODUCT_UNIT_REQUIRED(3011, "Product unit is required", HttpStatus.BAD_REQUEST),
    NOT_PRODUCT_OWNER(3012, "You do not have permission to modify this product", HttpStatus.FORBIDDEN),
    USER_NOT_A_SHOP(3013, "User is not registered as a shop/farmer", HttpStatus.FORBIDDEN),
    DISCOUNT_PERCENT_INVALID(3014, "Discount percent must be between 0 and 100", HttpStatus.BAD_REQUEST),

    // Product image errors
    PRODUCT_IMAGE_NOT_FOUND(3015, "Product image not found", HttpStatus.NOT_FOUND),
    IMAGE_URL_REQUIRED(3016, "Image URL is required", HttpStatus.BAD_REQUEST),
    IMAGE_URL_TOO_LONG(3017, "Image URL must not exceed 500 characters", HttpStatus.BAD_REQUEST),
    IMAGE_NOT_BELONG_TO_PRODUCT(3018, "Image does not belong to the specified product", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_PENDING(3019, "Product is not pending approval", HttpStatus.BAD_REQUEST),
    REJECT_REASON_REQUIRED(3020, "Rejection reason is required", HttpStatus.BAD_REQUEST),
    CANNOT_ACTIVATE_DIRECTLY(3021, "Shop cannot activate product directly; requires admin approval", HttpStatus.FORBIDDEN),
    IMAGE_NOT_AGRICULTURAL_PRODUCE(3022, "Ảnh sản phẩm không phải là nông sản hoặc không phù hợp", HttpStatus.BAD_REQUEST),
    IMAGE_MODERATION_FAILED(3023, "Lỗi trong quá trình kiểm duyệt ảnh", HttpStatus.INTERNAL_SERVER_ERROR),
    MAX_PRODUCT_IMAGES_EXCEEDED(3024, "Mỗi sản phẩm chỉ được tải lên tối đa 8 hình ảnh", HttpStatus.BAD_REQUEST),

    // Upload & File errors
    FILE_INVALID(4001, "Invalid image file", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(4002, "File size exceeds limit", HttpStatus.BAD_REQUEST),
    UPLOAD_FAILED(4003, "Failed to upload file to Cloudinary", HttpStatus.INTERNAL_SERVER_ERROR),

    // Mystery Box errors (Series 5000)
    BOX_TYPE_REQUIRED(5001, "Box type is required", HttpStatus.BAD_REQUEST),
    BOX_TYPE_TOO_LONG(5002, "Box type must not exceed 50 characters", HttpStatus.BAD_REQUEST),
    MYSTERY_BOX_PRICE_REQUIRED(5003, "Price is required", HttpStatus.BAD_REQUEST),
    MYSTERY_BOX_PRICE_INVALID(5004, "Price must be greater than or equal to 0", HttpStatus.BAD_REQUEST),
    MYSTERY_BOX_QUANTITY_REQUIRED(5005, "Total quantity is required", HttpStatus.BAD_REQUEST),
    MYSTERY_BOX_QUANTITY_INVALID(5006, "Total quantity must be at least 1", HttpStatus.BAD_REQUEST),
    MYSTERY_BOX_ITEMS_REQUIRED(5007, "At least one product item is required in the mystery box", HttpStatus.BAD_REQUEST),
    PRODUCT_ID_REQUIRED(5008, "Product ID in mystery box item is required", HttpStatus.BAD_REQUEST),
    PRODUCT_QUANTITY_REQUIRED(5009, "Product quantity in mystery box item is required", HttpStatus.BAD_REQUEST),
    PRODUCT_QUANTITY_INVALID(5010, "Product quantity in mystery box item must be at least 1", HttpStatus.BAD_REQUEST),
    DUPLICATE_PRODUCT_IN_BOX(5011, "Duplicate product found in mystery box items", HttpStatus.BAD_REQUEST),
    MYSTERY_BOX_NOT_FOUND(5012, "Mystery box not found", HttpStatus.NOT_FOUND),
    NOT_MYSTERY_BOX_OWNER(5013, "You do not have permission to modify this mystery box", HttpStatus.FORBIDDEN),
    PRODUCT_NOT_BELONG_TO_SHOP(5014, "One or more products do not belong to your shop", HttpStatus.FORBIDDEN),
    STOCK_INSUFFICIENT(5015, "Insufficient stock for product in mystery box", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_ACTIVE(5016, "One or more products are not active", HttpStatus.BAD_REQUEST),
    TOTAL_QUANTITY_LESS_THAN_SOLD(5017, "Total quantity cannot be less than already sold quantity", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_SOLD_MYSTERY_BOX(5018, "Cannot delete mystery box that has already been purchased. Please deactivate it instead", HttpStatus.BAD_REQUEST),
    SHOP_NOT_ACTIVE(5019, "Shop account is not active or approved", HttpStatus.FORBIDDEN),
    DESCRIPTION_TOO_LONG(5020, "Description must not exceed 500 characters", HttpStatus.BAD_REQUEST),
    NOTE_TOO_LONG(5021, "Note must not exceed 255 characters", HttpStatus.BAD_REQUEST),

    // Build Combo errors (Series 6000)
    COMBO_NAME_REQUIRED(6001, "Combo name is required", HttpStatus.BAD_REQUEST),
    COMBO_NAME_TOO_LONG(6002, "Combo name must not exceed 255 characters", HttpStatus.BAD_REQUEST),
    COMBO_PRICE_REQUIRED(6003, "Combo price is required", HttpStatus.BAD_REQUEST),
    COMBO_PRICE_INVALID(6004, "Combo price must be greater than or equal to 0", HttpStatus.BAD_REQUEST),
    COMBO_QUANTITY_REQUIRED(6005, "Total quantity is required", HttpStatus.BAD_REQUEST),
    COMBO_QUANTITY_INVALID(6006, "Total quantity must be at least 1", HttpStatus.BAD_REQUEST),
    COMBO_ITEMS_REQUIRED(6007, "At least one product item is required in the combo", HttpStatus.BAD_REQUEST),
    MEAL_TYPE_REQUIRED(6008, "Meal type is required", HttpStatus.BAD_REQUEST),
    DUPLICATE_PRODUCT_IN_COMBO(6009, "Duplicate product found in combo items", HttpStatus.BAD_REQUEST),
    COMBO_NOT_FOUND(6010, "Build combo not found", HttpStatus.NOT_FOUND),
    NOT_COMBO_OWNER(6011, "You do not have permission to modify this combo", HttpStatus.FORBIDDEN),
    CANNOT_DELETE_SOLD_COMBO(6012, "Cannot delete combo that has already been purchased. Please deactivate it instead", HttpStatus.BAD_REQUEST),
    COMBO_TYPE_TOO_LONG(6013, "Combo type must not exceed 50 characters", HttpStatus.BAD_REQUEST),
    COMBO_REGION_TOO_LONG(6014, "Region must not exceed 100 characters", HttpStatus.BAD_REQUEST),
    IMAGE_URL_TOO_LONG_COMBO(6015, "Image URL must not exceed 500 characters", HttpStatus.BAD_REQUEST),

    // Order errors (Series 7000)
    ORDER_NOT_FOUND(7001, "Order not found", HttpStatus.NOT_FOUND),
    INVALID_ORDER_STATE(7002, "Invalid order state transition", HttpStatus.BAD_REQUEST),
    SHOP_NOT_FOUND(7003, "Shop not found or is currently inactive", HttpStatus.BAD_REQUEST),

    // Cart Errors (Series 8000)
    CART_ITEM_NOT_FOUND(8001, "Cart item not found", HttpStatus.NOT_FOUND),
    OUT_OF_STOCK(8002, "Not enough stock available", HttpStatus.BAD_REQUEST),
    INVALID_ITEM_TYPE(8003, "Invalid item type", HttpStatus.BAD_REQUEST),

    // Voucher Errors (Series 9000)
    VOUCHER_CODE_EXISTED(9001, "Voucher code already exists", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_FOUND(9002, "Voucher not found", HttpStatus.NOT_FOUND),
    VOUCHER_INVALID(9003, "Voucher is inactive or expired", HttpStatus.BAD_REQUEST),
    VOUCHER_OUT_OF_STOCK(9004, "Voucher is out of stock (claim limit reached)", HttpStatus.BAD_REQUEST),
    VOUCHER_LIMIT_REACHED(9005, "You have reached the maximum claim limit for this voucher", HttpStatus.BAD_REQUEST),

    // Blog / Forum Errors (Series 10000)
    BLOG_NOT_FOUND(10001, "Blog post not found", HttpStatus.NOT_FOUND),
    NOT_BLOG_OWNER(10002, "You do not have permission to modify this blog post", HttpStatus.FORBIDDEN),
    COMMENT_NOT_FOUND(10003, "Comment not found", HttpStatus.NOT_FOUND),
    NOT_COMMENT_OWNER(10004, "You do not have permission to delete this comment", HttpStatus.FORBIDDEN),

    // Payment Errors (Series 7500)
    PAYMENT_NOT_FOUND(7501, "Payment not found", HttpStatus.NOT_FOUND),
    PAYMENT_FAILED(7502, "Payment creation failed", HttpStatus.BAD_REQUEST),
    PAYOS_WEBHOOK_INVALID(7503, "Invalid PayOS webhook signature", HttpStatus.BAD_REQUEST),
    ORDER_NOT_PAYABLE(7504, "Order is not in payable state", HttpStatus.BAD_REQUEST),
    BATCH_NOT_FOUND(7505, "Checkout batch not found", HttpStatus.NOT_FOUND),

    // Saved Address Errors (Series 9500)
    ADDRESS_NOT_FOUND(9501, "Saved address not found", HttpStatus.NOT_FOUND),
    MAX_ADDRESSES_REACHED(9502, "You have reached the maximum limit of 10 saved addresses", HttpStatus.BAD_REQUEST),
    ADDRESS_UNAUTHORIZED(9503, "You do not have permission to access or modify this address", HttpStatus.FORBIDDEN)
    ;

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
