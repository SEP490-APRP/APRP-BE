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
    EMAIL_INVALID(1006, "EMAIL_INVALID", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED(1009, "EMAIL_REQUIRED", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1007, "PASSWORD_INVALID", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1008, "PASSWORD_REQUIRED", HttpStatus.BAD_REQUEST),

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
    IMAGE_NOT_BELONG_TO_PRODUCT(3018, "Image does not belong to the specified product", HttpStatus.BAD_REQUEST)
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
