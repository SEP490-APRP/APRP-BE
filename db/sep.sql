-- ==============================================================================
-- CAPNONG (SEP490) - FULL DATABASE SCHEMA & SEED DATA (sep.sql)
-- Chuẩn hóa theo 100% Entity Spring Boot (APRP-BE)
-- Tham khảo cấu trúc và dữ liệu từ swd.sql và exe.sql
-- Mật khẩu mặc định tất cả tài khoản test: Admin@123 (hoặc 123456)
-- BCrypt hash: $2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K (Admin@123)
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS DB_CapNong CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE DB_CapNong;

-- Đảm bảo toàn bộ session, client và kết nối đều sử dụng utf8mb4 (chuẩn tiếng Việt đầy đủ)
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. DROP ALL EXISTING TABLES
DROP TABLE IF EXISTS plan_meal_items;
DROP TABLE IF EXISTS plan_meals;
DROP TABLE IF EXISTS plan_days;
DROP TABLE IF EXISTS plan_generation_requests;
DROP TABLE IF EXISTS build_plans;
DROP TABLE IF EXISTS chat_messages;
DROP TABLE IF EXISTS conversations;
DROP TABLE IF EXISTS chat_bot;
DROP TABLE IF EXISTS bot_knowledge;
DROP TABLE IF EXISTS blogs;
DROP TABLE IF EXISTS user_notifications;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS shipper_locations;
DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS review_reactions;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS return_requests;
DROP TABLE IF EXISTS voucher_usages;
DROP TABLE IF EXISTS voucher_users;
DROP TABLE IF EXISTS vouchers;
DROP TABLE IF EXISTS withdraw_requests;
DROP TABLE IF EXISTS wallets;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS order_details;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS checkout_batches;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS product_combo;
DROP TABLE IF EXISTS build_combos;
DROP TABLE IF EXISTS product_mystery;
DROP TABLE IF EXISTS mystery_boxes;
DROP TABLE IF EXISTS product_allergens;
DROP TABLE IF EXISTS moderation_labels;
DROP TABLE IF EXISTS moderation_results;
DROP TABLE IF EXISTS nutrition_info;
DROP TABLE IF EXISTS product_images;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS profile_update_requests;
DROP TABLE IF EXISTS user_health_conditions;
DROP TABLE IF EXISTS user_allergies;
DROP TABLE IF EXISTS health_conditions;
DROP TABLE IF EXISTS allergens;
DROP TABLE IF EXISTS otp_verification;
DROP TABLE IF EXISTS invalidated_token;
DROP TABLE IF EXISTS saved_addresses;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

-- ==============================================================================
-- 2. CREATE TABLES (MATCHING APRP-BE JPA ENTITIES)
-- ==============================================================================

-- 2.1 ROLES
CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.2 USERS
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name TEXT,
    phone_number VARCHAR(50),
    address VARCHAR(255),
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING') DEFAULT 'ACTIVE',
    shop_name VARCHAR(255),
    shop_active TINYINT(1) DEFAULT 1,
    bank_account VARCHAR(100),
    bank_account_holder VARCHAR(100),
    bank_name VARCHAR(100),
    rating_average DOUBLE DEFAULT 0.0,
    description VARCHAR(500),
    logo_url VARCHAR(500),
    achievement VARCHAR(255),
    license VARCHAR(100),
    license_image_url VARCHAR(500),
    vehicle_number VARCHAR(50),
    vehicle_doc_image_url VARCHAR(500),
    diet_preference ENUM('VEGETARIAN', 'NON_VEGETARIAN', 'VEGAN', 'FLEXITARIAN', 'PESCATARIAN'),
    loyalty_points DECIMAL(18,2) DEFAULT 0.00,
    total_rescued_weight DECIMAL(18,2) DEFAULT 0.00,
    locked_at DATETIME,
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    role_id INT,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.2b SAVED ADDRESSES (SỔ ĐỊA CHỈ KHÁCH HÀNG)
CREATE TABLE saved_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    label VARCHAR(50),
    recipient_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address TEXT NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    is_default TINYINT(1) DEFAULT 0,
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_saved_addr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.3 INVALIDATED TOKEN
CREATE TABLE invalidated_token (
    id VARCHAR(255) PRIMARY KEY,
    expiry_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.4 OTP VERIFICATION
CREATE TABLE otp_verification (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expiration_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.5 ALLERGENS & HEALTH CONDITIONS
CREATE TABLE allergens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE health_conditions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_allergies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    allergen_id INT NOT NULL,
    CONSTRAINT fk_user_allergies_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_allergies_allergen FOREIGN KEY (allergen_id) REFERENCES allergens(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_health_conditions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    condition_id INT NOT NULL,
    CONSTRAINT fk_user_health_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_health_cond FOREIGN KEY (condition_id) REFERENCES health_conditions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE profile_update_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    raw_input_text TEXT,
    extracted_diff LONGTEXT,
    confirmed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_update_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.6 CATEGORIES
CREATE TABLE categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.7 PRODUCTS
CREATE TABLE products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    shop_id INT NOT NULL,
    category_id INT,
    product_name VARCHAR(255) NOT NULL,
    unit VARCHAR(50),
    selling_price DECIMAL(18,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    expiry_date DATE,
    reject_reason TEXT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    discount_percent INT DEFAULT 0,
    discount_start_date DATETIME,
    discount_end_date DATETIME,
    CONSTRAINT fk_products_shop FOREIGN KEY (shop_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.8 PRODUCT IMAGES
CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary TINYINT(1) DEFAULT 0,
    display_order INT DEFAULT 0,
    is_approved TINYINT(1) NOT NULL DEFAULT 1,
    is_flagged TINYINT(1) NOT NULL DEFAULT 0,
    flag_reason VARCHAR(500) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.9 NUTRITION INFO
CREATE TABLE nutrition_info (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL UNIQUE,
    calories DECIMAL(10,2),
    protein_g DECIMAL(10,2),
    carbs_g DECIMAL(10,2),
    fat_g DECIMAL(10,2),
    CONSTRAINT fk_nutrition_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.10 CONTENT MODERATION (AI)
CREATE TABLE moderation_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL UNIQUE,
    is_flagged TINYINT(1) DEFAULT 0,
    overall_confidence DECIMAL(5,2),
    checked_at DATETIME,
    raw_response LONGTEXT,
    CONSTRAINT fk_moderation_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE moderation_labels (
    id INT AUTO_INCREMENT PRIMARY KEY,
    moderation_result_id INT NOT NULL,
    label_name VARCHAR(100),
    parent_label VARCHAR(100),
    confidence DECIMAL(5,2),
    CONSTRAINT fk_moderation_labels_res FOREIGN KEY (moderation_result_id) REFERENCES moderation_results(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_allergens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    allergen_id INT NOT NULL,
    CONSTRAINT fk_product_allergens_prod FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_product_allergens_allg FOREIGN KEY (allergen_id) REFERENCES allergens(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.11 MYSTERY BOXES
CREATE TABLE mystery_boxes (
    mystery_id INT AUTO_INCREMENT PRIMARY KEY,
    shop_id INT NOT NULL,
    box_type VARCHAR(50),
    image_url VARCHAR(500),
    price DECIMAL(18,2),
    description VARCHAR(500),
    note VARCHAR(255),
    is_active TINYINT(1) DEFAULT 1,
    sold_quantity INT DEFAULT 0,
    total_quantity INT DEFAULT 0,
    CONSTRAINT fk_mystery_shop FOREIGN KEY (shop_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_mystery (
    product_mystery_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    mystery_id INT NOT NULL,
    product_quantity INT,
    CONSTRAINT fk_prod_myst_prod FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_prod_myst_myst FOREIGN KEY (mystery_id) REFERENCES mystery_boxes(mystery_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.12 BUILD COMBOS
CREATE TABLE build_combos (
    combo_id INT AUTO_INCREMENT PRIMARY KEY,
    shop_id INT NOT NULL,
    combo_name VARCHAR(255) NOT NULL,
    price DECIMAL(18,2),
    description VARCHAR(500),
    type VARCHAR(255),
    meal_type ENUM('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'),
    region VARCHAR(50),
    total_quantity INT DEFAULT 0,
    sold_quantity INT DEFAULT 0,
    image_url VARCHAR(500),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    cooking_suggestion VARCHAR(500),
    CONSTRAINT fk_combos_shop FOREIGN KEY (shop_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_combo (
    product_combo_id INT AUTO_INCREMENT PRIMARY KEY,
    combo_id INT NOT NULL,
    product_id INT NOT NULL,
    product_quantity INT,
    CONSTRAINT fk_prod_combo_combo FOREIGN KEY (combo_id) REFERENCES build_combos(combo_id) ON DELETE CASCADE,
    CONSTRAINT fk_prod_combo_prod FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.13 CARTS & CART ITEMS
CREATE TABLE carts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cart_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cart_id INT NOT NULL,
    product_id INT,
    mystery_box_id INT,
    build_combo_id INT,
    quantity INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_prod FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_myst FOREIGN KEY (mystery_box_id) REFERENCES mystery_boxes(mystery_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_combo FOREIGN KEY (build_combo_id) REFERENCES build_combos(combo_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.14 CHECKOUT BATCHES
CREATE TABLE checkout_batches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    payos_order_code VARCHAR(255),
    payment_method VARCHAR(50),
    status VARCHAR(32) DEFAULT 'PENDING',
    total_amount DECIMAL(18,2),
    batch_discount_amount DECIMAL(18,2) DEFAULT 0.00,
    checkout_url TEXT,
    qr_code_url TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_batch_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.15 ORDERS
CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    shop_id INT NOT NULL,
    shipper_id INT,
    checkout_batch_id INT,
    shipping_fee DECIMAL(18,2) DEFAULT 0.00,
    recipient_name VARCHAR(50),
    recipient_phone VARCHAR(20),
    shipping_address VARCHAR(255),
    estimated_delivery_date DATETIME,
    note VARCHAR(500),
    status VARCHAR(32) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(18,2) NOT NULL,
    payment_method VARCHAR(50),
    shipping_discount_amount DECIMAL(18,2) DEFAULT 0.00,
    batch_discount_amount DECIMAL(18,2) DEFAULT 0.00,
    shipping_latitude DOUBLE,
    shipping_longitude DOUBLE,
    shop_latitude DOUBLE,
    shop_longitude DOUBLE,
    refund_amount DECIMAL(18,2) DEFAULT 0.00,
    refund_reason VARCHAR(255),
    reward_points DECIMAL(10,2) DEFAULT 0.00,
    cod_amount DECIMAL(19,2) DEFAULT 0.00,
    cod_settled TINYINT(1) DEFAULT 0,
    cod_settled_at DATETIME,
    cod_cash_confirmed_at DATETIME,
    cod_payment_option ENUM('FULL_COD', 'DEPOSIT'),
    cod_payment_status ENUM('UNPAID', 'PARTIALLY_PAID', 'PAID', 'REFUNDED'),
    cod_prepaid_amount DECIMAL(18,2) DEFAULT 0.00,
    cod_refunded_at DATETIME,
    shipper_paid_at DATETIME,
    picked_up_at DATETIME,
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_shop FOREIGN KEY (shop_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_shipper FOREIGN KEY (shipper_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_orders_batch FOREIGN KEY (checkout_batch_id) REFERENCES checkout_batches(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.16 ORDER DETAILS
CREATE TABLE order_details (
    order_detail_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT,
    mystery_id INT,
    combo_id INT,
    quantity INT NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    item_type VARCHAR(50),
    is_requested_return TINYINT(1) DEFAULT 0,
    CONSTRAINT fk_order_details_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_details_prod FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE SET NULL,
    CONSTRAINT fk_order_details_myst FOREIGN KEY (mystery_id) REFERENCES mystery_boxes(mystery_id) ON DELETE SET NULL,
    CONSTRAINT fk_order_details_combo FOREIGN KEY (combo_id) REFERENCES build_combos(combo_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.17 PAYMENTS
CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,
    payment_date DATETIME,
    amount DECIMAL(18,2),
    payment_gateway VARCHAR(50),
    status ENUM('PENDING', 'PAID', 'CANCELLED', 'FAILED') DEFAULT 'PENDING',
    image VARCHAR(500),
    payos_order_code VARCHAR(255),
    checkout_url TEXT,
    qr_code_url TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.18 TRANSACTIONS
CREATE TABLE transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    payment_id INT NOT NULL,
    payment_gateway VARCHAR(50),
    amount DECIMAL(18,2),
    balance_after DECIMAL(18,2),
    status ENUM('SUCCESS', 'FAILED', 'PENDING'),
    content VARCHAR(500),
    transaction_type ENUM('PAYMENT', 'REFUND', 'PAYOUT', 'COMMISSION'),
    reference_id VARCHAR(100),
    CONSTRAINT fk_trans_payment FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.19 WALLETS
CREATE TABLE wallets (
    wallet_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    status ENUM('ACTIVE', 'LOCKED', 'CLOSED') DEFAULT 'ACTIVE',
    type VARCHAR(50),
    total_balance DECIMAL(18,2) DEFAULT 0.00,
    frozen_balance DECIMAL(18,2) DEFAULT 0.00,
    total_revenue_all_time DECIMAL(18,2) DEFAULT 0.00,
    total_withdrawn DECIMAL(18,2) DEFAULT 0.00,
    commission_wallet DECIMAL(18,2) DEFAULT 0.00,
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.20 WITHDRAW REQUESTS
CREATE TABLE withdraw_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    wallet_id INT NOT NULL,
    admin_id INT,
    reason VARCHAR(255),
    admin_note VARCHAR(255),
    processed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'PROCESSING') DEFAULT 'PENDING',
    receive_amount DECIMAL(18,2),
    amount DECIMAL(18,2),
    fee DECIMAL(18,2) DEFAULT 0.00,
    bank_name VARCHAR(100),
    bank_account_number VARCHAR(50),
    bank_account_holder VARCHAR(100),
    payout_status ENUM('PENDING', 'SUCCESS', 'FAILED'),
    payout_provider VARCHAR(50),
    payout_order_code VARCHAR(100),
    payout_transaction_id VARCHAR(100),
    payout_checkout_url TEXT,
    payout_qr_url TEXT,
    payout_message VARCHAR(255),
    payout_at DATETIME,
    CONSTRAINT fk_withdraw_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id) ON DELETE CASCADE,
    CONSTRAINT fk_withdraw_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.21 VOUCHERS
CREATE TABLE vouchers (
    voucher_id INT AUTO_INCREMENT PRIMARY KEY,
    shop_id INT NOT NULL,
    voucher_code VARCHAR(20) NOT NULL,
    discount_value DECIMAL(18,2),
    max_discount DECIMAL(18,2),
    min_order_value DECIMAL(18,2),
    usage_limit INT,
    expiry_date DATETIME,
    voucher_type ENUM('FIXED_AMOUNT', 'PERCENTAGE', 'FREE_SHIPPING'),
    voucher_scope ENUM('PLATFORM', 'SHOP', 'SYSTEM', 'WELCOME') DEFAULT 'SHOP',
    voucher_target ENUM('ORDER', 'SHIPPING') DEFAULT 'ORDER',
    claim_limit INT,
    claimed_count INT DEFAULT 0,
    used_count INT DEFAULT 0,
    per_user_limit INT DEFAULT 1,
    active TINYINT(1) DEFAULT 1,
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    auto_created_reason VARCHAR(100),
    auto_created_from_order_id INT,
    CONSTRAINT fk_vouchers_shop FOREIGN KEY (shop_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE voucher_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    voucher_id INT NOT NULL,
    quantity INT DEFAULT 1,
    quantity_used INT DEFAULT 0,
    claim_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vouch_users_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_vouch_users_vouch FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE voucher_usages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    user_id INT NOT NULL,
    voucher_id INT NOT NULL,
    discount_amount DECIMAL(18,2),
    used_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vouch_usage_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_vouch_usage_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_vouch_usage_vouch FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.22 RETURN REQUESTS
CREATE TABLE return_requests (
    return_id INT AUTO_INCREMENT PRIMARY KEY,
    order_detail_id INT NOT NULL,
    shop_id INT NOT NULL,
    buyer_id INT NOT NULL,
    reason VARCHAR(255),
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED') DEFAULT 'PENDING',
    refund_amount DECIMAL(18,2),
    shop_response VARCHAR(1000),
    admin_remark VARCHAR(1000),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_returns_detail FOREIGN KEY (order_detail_id) REFERENCES order_details(order_detail_id) ON DELETE CASCADE,
    CONSTRAINT fk_returns_shop FOREIGN KEY (shop_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_returns_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.23 REVIEWS & REACTIONS
CREATE TABLE reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    shop_id INT,
    order_detail_id INT,
    product_id INT,
    rating_star DECIMAL(5,2),
    comment VARCHAR(255),
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    reply_from_shop VARCHAR(255),
    CONSTRAINT fk_reviews_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_shop FOREIGN KEY (shop_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_reviews_detail FOREIGN KEY (order_detail_id) REFERENCES order_details(order_detail_id) ON DELETE SET NULL,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review_reactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    review_id INT NOT NULL,
    buyer_id INT NOT NULL,
    reaction_type ENUM('LIKE', 'DISLIKE') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rev_react_review FOREIGN KEY (review_id) REFERENCES reviews(review_id) ON DELETE CASCADE,
    CONSTRAINT fk_rev_react_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.24 IMAGES (POLYMORPHIC FOR WITHDRAW / RETURN / REVIEW)
CREATE TABLE images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    image_url VARCHAR(500) NOT NULL,
    media_type ENUM('IMAGE', 'VIDEO') DEFAULT 'IMAGE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    with_draw_id INT,
    return_id INT,
    review_id INT,
    CONSTRAINT fk_images_withdraw FOREIGN KEY (with_draw_id) REFERENCES withdraw_requests(request_id) ON DELETE CASCADE,
    CONSTRAINT fk_images_return FOREIGN KEY (return_id) REFERENCES return_requests(return_id) ON DELETE CASCADE,
    CONSTRAINT fk_images_review FOREIGN KEY (review_id) REFERENCES reviews(review_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.25 SHIPPER LOCATIONS
CREATE TABLE shipper_locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,
    shipper_id INT NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ship_loc_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE SET NULL,
    CONSTRAINT fk_ship_loc_shipper FOREIGN KEY (shipper_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.26 NOTIFICATIONS
CREATE TABLE notifications (
    noti_id INT AUTO_INCREMENT PRIMARY KEY,
    admin_id INT,
    title VARCHAR(50),
    message TEXT,
    evidence VARCHAR(500),
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    receiver_type VARCHAR(100),
    CONSTRAINT fk_notif_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    notification_id INT NOT NULL,
    is_deleted TINYINT(1) DEFAULT 0,
    is_read TINYINT(1) DEFAULT 0,
    CONSTRAINT fk_unotif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_unotif_notif FOREIGN KEY (notification_id) REFERENCES notifications(noti_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.27 BLOGS
CREATE TABLE blogs (
    blog_id INT AUTO_INCREMENT PRIMARY KEY,
    shop_id INT NOT NULL,
    title VARCHAR(50) NOT NULL,
    content TEXT,
    picture_url VARCHAR(500),
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    category VARCHAR(100),
    views INT DEFAULT 0,
    CONSTRAINT fk_blogs_shop FOREIGN KEY (shop_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.28 CHAT & BOT
CREATE TABLE conversations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_key VARCHAR(255),
    user1_id INT NOT NULL,
    user2_id INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_message_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conv_u1 FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_conv_u2 FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT NOT NULL,
    sender_id INT NOT NULL,
    content TEXT,
    is_read TINYINT(1) DEFAULT 0,
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_msg_conv FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_bot (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    role VARCHAR(10),
    content TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bot_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bot_knowledge (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50),
    title VARCHAR(255),
    content TEXT,
    keywords TEXT,
    active TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.29 BUILD PLANS & MEALS (AI NUTRITION)
CREATE TABLE build_plans (
    plan_id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    plan_name VARCHAR(255),
    number_of_days INT,
    number_of_people INT,
    meal_type VARCHAR(50),
    target_budget DECIMAL(18,2),
    status VARCHAR(50) DEFAULT 'AI_DRAFT',
    source VARCHAR(50) DEFAULT 'AI',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_plan_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE plan_days (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plan_id INT NOT NULL,
    day_index INT,
    date DATE,
    CONSTRAINT fk_plday_plan FOREIGN KEY (plan_id) REFERENCES build_plans(plan_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE plan_meals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plan_day_id INT NOT NULL,
    meal_type ENUM('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'),
    sort_order INT,
    completed TINYINT(1) DEFAULT 0,
    CONSTRAINT fk_plmeal_day FOREIGN KEY (plan_day_id) REFERENCES plan_days(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE plan_meal_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plan_meal_id INT NOT NULL,
    combo_id INT,
    quantity INT DEFAULT 1,
    unit_price_snapshot DECIMAL(18,2),
    is_ai_suggested TINYINT(1) DEFAULT 1,
    position INT,
    user_override TINYINT(1) DEFAULT 0,
    CONSTRAINT fk_plitem_meal FOREIGN KEY (plan_meal_id) REFERENCES plan_meals(id) ON DELETE CASCADE,
    CONSTRAINT fk_plitem_combo FOREIGN KEY (combo_id) REFERENCES build_combos(combo_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE plan_generation_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plan_id INT,
    buyer_id INT NOT NULL,
    requested_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    input_params LONGTEXT,
    raw_ai_response LONGTEXT,
    CONSTRAINT fk_plgen_plan FOREIGN KEY (plan_id) REFERENCES build_plans(plan_id) ON DELETE SET NULL,
    CONSTRAINT fk_plgen_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- 3. SEED DATA (CHỌN LỌC TỪ SWD.SQL & EXE.SQL, TƯƠNG THÍCH HOÀN TOÀN)
-- ==============================================================================

-- 3.1 ROLES
INSERT INTO roles (id, name, description) VALUES
(1, 'BUYER', 'Role dành cho Người mua hàng'),
(2, 'SHOP', 'Role dành cho Cửa hàng / Nhà vườn CapNong'),
(3, 'SHIPPER', 'Role dành cho Người giao hàng'),
(4, 'ADMIN', 'Role dành cho Quản trị viên hệ thống')
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description);

-- 3.2 USERS
-- Mật khẩu mặc định: Admin@123 ($2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K)
INSERT INTO users (id, email, password, role_id, status, full_name, phone_number, address, shop_name, shop_active, bank_name, bank_account, bank_account_holder, rating_average, description, logo_url, achievement, license, vehicle_number, diet_preference, create_at) VALUES
(1, 'admin@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 4, 'ACTIVE', 'Quản Trị Viên Hệ Thống', '0900000000', '1 Công Xã Paris, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh', NULL, 0, 'Vietcombank', '0011001234567', 'CAPNONG ADMIN', 5.0, 'Quản trị viên toàn hệ thống CapNong', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', 'SuperAdmin', NULL, NULL, NULL, NOW()),

(2, 'buyer1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 1, 'ACTIVE', 'Ngô Gia Sang (Khách mua)', '0911111111', '268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh', NULL, 0, 'VietinBank', '101888888888', 'NGO GIA SANG', 5.0, 'Khách hàng thân thiết', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', NULL, NULL, NULL, 'VEGETARIAN', NOW()),

(3, 'buyer2@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 1, 'ACTIVE', 'Trần Thị Mai', '0911111112', 'Chợ Bến Thành, Đường Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh', NULL, 0, 'MBBank', '970422888888', 'TRAN THI MAI', 5.0, 'Khách hàng', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', NULL, NULL, NULL, 'VEGAN', NOW()),

(4, 'shop1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 2, 'ACTIVE', 'Trần Văn Kiệt (Nhà Vườn 1)', '0933333333', 'Số 1 Võ Văn Ngân, Phường Linh Chiểu, Thành phố Thủ Đức, TP. Hồ Chí Minh', 'Nông Sản Xấu Mã - Vườn Đà Lạt', 1, 'Vietcombank', '0071000123456', 'TRAN VAN KIET', 4.9, 'Chuyên rau củ quả organic sạch từ Đà Lạt và các vùng nông nghiệp sạch chuẩn VietGAP.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252594/brands/srflhqfol1ckwavsaa1n.jpg', 'Hạt Giống Vàng', NULL, NULL, NULL, NOW()),

(5, 'shop2@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 2, 'ACTIVE', 'Lê Thị Ngọc (Nhà Vườn 2)', '0944444444', 'Đường Nguyễn Du, Khu Đô thị Đại học Quốc gia, Dĩ An, Bình Dương', 'Vườn Xanh Bình Dương & Miền Tây', 1, 'Techcombank', '19034567890123', 'LE THI NGOC', 4.8, 'Chuyên rau củ quả tươi miền Nam, trái cây đặc sản miền Tây chất lượng cao, thu hái trong ngày.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252337/brands/t9muel0yqmqfzfu3ndij.jpg', 'Nông Dân Tinh Hoa', NULL, NULL, NULL, NOW()),

(6, 'shipper1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 3, 'ACTIVE', 'Nguyễn Văn Hoàng (Shipper)', '0922222222', 'Số 1 Võ Văn Ngân, Phường Linh Chiểu, Thành phố Thủ Đức, TP. Hồ Chí Minh', NULL, 0, 'VietinBank', '101899999999', 'NGUYEN VAN HOANG', 4.9, 'Shipper nhiệt tình chuyên nghiệp thùng cách nhiệt bảo quản rau tươi', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252921/brands/xu1pv9ty2njacb3bhozh.jpg', NULL, 'A1-99999999', '59-X1 123.45', NULL, NOW())
ON DUPLICATE KEY UPDATE email=VALUES(email), diet_preference=VALUES(diet_preference);

-- 3.3 CATEGORIES
INSERT INTO categories (category_id, name, description) VALUES
(1, 'Rau lá', 'Các loại rau xanh tươi sạch: rau muống, cải, xà lách, rau thơm thu hoạch sáng sớm.'),
(2, 'Củ', 'Các loại củ tươi ngon: cà rốt, khoai tây, khoai lang, củ cải, hành tỏi Đà Lạt.'),
(3, 'Quả / Trái cây', 'Trái cây tươi đặc sản: chuối, bưởi da xanh, xoài cát, cam sành, dâu tây mọng nước.'),
(4, 'Nông sản hữu cơ VietGAP', 'Nông sản canh tác tự nhiên đạt chuẩn chứng nhận an toàn thực phẩm.'),
(5, 'Combo & Túi mù', 'Gói rau củ theo món ăn và túi mù nông sản tiết kiệm bảo vệ môi trường.')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 3.4 PRODUCTS (ĐƯỢC GẮN VỚI SHOP 4 VÀ SHOP 5)
INSERT INTO products (product_id, shop_id, category_id, product_name, unit, selling_price, stock_quantity, description, status, expiry_date, discount_percent) VALUES
-- Shop 1 (id: 4) - Vườn Đà Lạt
(101, 4, 1, 'Rau muống sạch Đà Lạt', 'bó', 12000, 100, 'Rau muống tươi xanh ngọn non mướt, thu hoạch sáng sớm từ vườn hữu cơ Đà Lạt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(102, 4, 1, 'Cải ngọt tươi giòn', 'bó', 15000, 80, 'Cải ngọt trồng theo hướng hữu cơ, lá mướt xanh không dập nát, phù hợp xào nấu canh.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 10),
(103, 4, 1, 'Xà lách mỡ giòn ngọt', 'túi', 18000, 60, 'Xà lách mỡ tươi rửa sạch ăn sống ngay, thích hợp làm salad giải nhiệt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 0),
(104, 4, 1, 'Cải bó xôi (Spinach)', 'túi', 22000, 50, 'Cải bó xôi giàu sắt và dinh dưỡng cho bé và cả gia đình.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(105, 4, 2, 'Cà rốt Đà Lạt hữu cơ', 'kg', 25000, 120, 'Cà rốt vỏ cam tự nhiên, ngọt đậm vị củ tươi vừa nhổ, giàu beta-carotene.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 5),
(106, 4, 2, 'Khoai tây vàng Đà Lạt', 'kg', 30000, 90, 'Khoai tây bùi thơm, ruột vàng ruộm, vỏ mỏng không sâu mọt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), 0),
(107, 4, 2, 'Khoai lang mật tím', 'kg', 28000, 75, 'Khoai lang mật tím dẻo ngọt, nướng hoặc hấp thơm lừng.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), 0),
(108, 4, 3, 'Cà chua bi ngọt mọng nước', 'kg', 35000, 60, 'Cà chua chín cây đỏ mọng, giàu vitamin C và chất chống oxy hóa lycopene.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(109, 4, 3, 'Dâu tây giống Nhật Đà Lạt', 'kg', 180000, 30, 'Dâu tây đỏ mọng hái sáng sớm tại nhà kính Đà Lạt, ngọt thanh chua nhẹ.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 15),
(209, 4, 4, 'Nấm đùi gà hữu cơ Đà Lạt', 'khay', 32000, 80, 'Nấm đùi gà thịt dày dai ngọt, giàu đạm thực vật và khoáng chất, tuyệt hảo cho món xào nướng lẩu.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(210, 4, 4, 'Nấm rơm tươi Đồng Nai', 'khay', 28000, 60, 'Nấm rơm tròn búp tươi ngon, vị ngọt thanh tự nhiên, phù hợp nấu súp, canh chay, kho quẹt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 0),
(211, 4, 4, 'Đậu hũ non organic đậu nành', 'hộp', 14000, 100, 'Đậu hũ non mềm mịn béo ngậy từ 100% đậu nành không biến đổi gen, giàu protein thực vật.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 0),
(214, 4, 1, 'Bông cải xanh (Broccoli) Đà Lạt', 'kg', 38000, 70, 'Bông cải xanh đậm màu tươi giòn, giàu chất chống oxy hóa và chất xơ cao.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 0),
(217, 4, 2, 'Bí ngòi xanh Đà Lạt', 'kg', 26000, 65, 'Bí ngòi xanh vỏ bóng giòn ngọt, ít calo giàu nước, phù hợp xào tỏi hoặc nấu canh.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(218, 4, 3, 'Ớt chuông đỏ ngọt Đà Lạt', 'kg', 42000, 55, 'Ớt chuông đỏ dày cùi giòn ngọt, giàu vitamin C gấp 3 lần cam, lý tưởng làm salad.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 0),

-- Shop 2 (id: 5) - Vườn Miền Tây
(201, 5, 1, 'Rau ngót vườn hữu cơ', 'bó', 10000, 80, 'Rau ngót lá nhỏ xanh đậm, nấu canh thịt bằm ngọt mát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 0),
(202, 5, 1, 'Mồng tơi ngọn non', 'bó', 10000, 90, 'Mồng tơi xanh non, thích hợp nấu canh cua rau đay mồng tơi thanh mát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 0),
(203, 5, 2, 'Bí đỏ hồ lô dẻo ngọt', 'kg', 22000, 70, 'Bí đỏ hồ lô hạt lép thịt dày, giàu vitamin A, nấu chè hầm canh đều ngon.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), 0),
(204, 5, 2, 'Củ cải trắng tươi mát', 'kg', 18000, 85, 'Củ cải trắng vị ngọt giòn thanh, dùng muối dưa, nấu súp hầm xương.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 0),
(205, 5, 3, 'Bưởi da xanh Bến Tre', 'kg', 65000, 50, 'Bưởi da xanh tép hồng mọng nước, vị ngọt thanh không chua, múi tróc vỏ.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), 0),
(206, 5, 3, 'Cam sành Tiền Giang', 'kg', 35000, 150, 'Cam sành vỏ mỏng mọng nước, vị chua ngọt tự nhiên giàu vitamin C giải khát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 0),
(207, 5, 3, 'Xoài cát Hòa Lộc Tiền Giang', 'kg', 85000, 40, 'Xoài cát Hòa Lộc nức tiếng thịt vàng ươm, thơm ngát, vị ngọt đậm đà.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 10),
(208, 5, 3, 'Dưa hấu ruột đỏ ngọt lịm', 'kg', 14000, 180, 'Dưa hấu vỏ mỏng ruột đỏ đặc ruột, ngọt lịm giải nhiệt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 0),
(212, 5, 2, 'Hạt sen tươi Đồng Tháp Mười', 'gói', 45000, 50, 'Hạt sen tươi bỏ tâm không đắng, vị bùi bở thơm mát, bổ tâm an thần, tốt cho giấc ngủ.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 0),
(213, 5, 2, 'Bắp ngọt vàng Tiền Giang', 'trái', 12000, 120, 'Bắp ngọt hạt đều căng mọng, vị ngọt tự nhiên đậm đà, luộc hay nấu canh đều ngon.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 0),
(215, 5, 3, 'Bơ sáp Đắk Lắk dẻo béo', 'kg', 45000, 90, 'Bơ sáp loại 1 thịt vàng dẻo quánh, giàu chất béo tốt không bão hòa và vitamin E.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(216, 5, 3, 'Chuối già Nam Mỹ hữu cơ', 'nải', 25000, 80, 'Chuối chín thơm ngon dẻo ngọt, nguồn kali và năng lượng lành mạnh tự nhiên.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 0)
ON DUPLICATE KEY UPDATE product_name=VALUES(product_name), selling_price=VALUES(selling_price);

-- 3.5 MYSTERY BOXES & BUILD COMBOS (REFERENCED FROM EXE.SQL / SWD.SQL)
-- Dữ liệu này giúp buyer page getMysteryBoxes / getCombos trả về data thực thay vì fallback mock.
INSERT INTO mystery_boxes (mystery_id, shop_id, box_type, image_url, price, description, note, is_active, total_quantity, sold_quantity) VALUES
(1, 4, 'Hộp Củ Bí Ẩn Đà Lạt', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg', 89000, 'Hộp củ bí ẩn từ vườn organic Đà Lạt, gồm 4-5 loại củ tươi ngon được chọn lọc kỹ càng.', 'Giao hàng trong ngày, bảo đảm tươi sạch', 1, 20, 5),
(2, 4, 'Hộp Trái Cây Dinh Dưỡng', 'https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop', 129000, 'Hộp trái cây bí ẩn gồm 3-4 loại quả tươi ngon, giàu vitamin và khoáng chất.', 'Giá trị thực tế cao hơn 30% so với giá bán', 1, 15, 3),
(3, 5, 'Hộp Củ Quả Miền Tây', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop', 75000, 'Hộp củ quả đặc sản miền Tây gồm 4-5 loại tươi xanh mát, phù hợp nấu canh và xào.', 'Thu hoạch buổi sáng, giao ngay trong ngày', 1, 25, 8),
(4, 5, 'Hộp Trái Cây Nhiệt Đới', 'https://images.unsplash.com/photo-1553279768-865429fa0078?w=400&h=300&fit=crop', 149000, 'Hộp trái cây nhiệt đới gồm 4-5 loại trái cây ngọt ngon, tươi mát cho mùa hè.', 'Phù hợp giải nhiệt mùa hè', 1, 15, 4)
ON DUPLICATE KEY UPDATE box_type=VALUES(box_type), price=VALUES(price), description=VALUES(description);

SET @box_shop1_1 = (SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Củ Bí Ẩn Đà Lạt' AND shop_id=4);
SET @box_shop1_2 = (SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Trái Cây Dinh Dưỡng' AND shop_id=4);
SET @box_shop2_1 = (SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Củ Quả Miền Tây' AND shop_id=5);
SET @box_shop2_2 = (SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Trái Cây Nhiệt Đới' AND shop_id=5);

INSERT INTO product_mystery (product_id, mystery_id, product_quantity) VALUES
(105, @box_shop1_1, 1), (106, @box_shop1_1, 1), (107, @box_shop1_1, 1), (104, @box_shop1_1, 1),
(108, @box_shop1_2, 1), (109, @box_shop1_2, 1), (102, @box_shop1_2, 1),
(203, @box_shop2_1, 1), (204, @box_shop2_1, 1), (206, @box_shop2_1, 1), (207, @box_shop2_1, 1),
(208, @box_shop2_2, 2), (205, @box_shop2_2, 1), (206, @box_shop2_2, 1)
ON DUPLICATE KEY UPDATE product_quantity=VALUES(product_quantity);

-- 20 COMBOS PHỤC VỤ ĐẦY ĐỦ CÁC BỮA (SÁNG, TRƯA, TỐI, PHỤ) & CÁC CHẾ ĐỘ ĂN (CHAY, EAT CLEAN, TIẾT KIỆM)
INSERT INTO build_combos (combo_id, shop_id, combo_name, price, description, type, meal_type, region, total_quantity, sold_quantity, image_url, status, cooking_suggestion) VALUES
-- BỮA SÁNG (BREAKFAST)
(1, 4, 'Combo Detox Củ Quả Sáng', 70000, 'Bộ củ quả giải độc buổi sáng: cà rốt tươi ngọt, cải ngọt giòn mát, khoai lang bùi ngọt và xà lách sạch.', 'Nước Ép & Luộc', 'BREAKFAST', 'MIEN_NAM', 50, 0, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916721/brands/uj1eoinkwvthfjvtpvpq.jpg', 'ACTIVE', 'Ép nước tươi uống giải độc hoặc luộc chấm muối mè; có thể ăn kèm 1 quả trứng luộc hoặc lát bánh mì nguyên cám.'),
(2, 4, 'Combo Salad Eat Clean Đà Lạt', 75000, 'Bộ rau củ cho bữa salad eat clean: xà lách mỡ, cà chua bi, cải bó xôi và ớt chuông đỏ giàu vitamin C.', 'Salad Eat Clean', 'BREAKFAST', 'DA_LAT', 50, 0, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917431/brands/yif2icgotbydg5qvzvbf.jpg', 'ACTIVE', 'Trộn trực tiếp với sốt mè rang hoặc dầu giấm balsamic; gợi ý thêm 100g ức gà áp chảo hoặc đậu hũ cắt hạt lựu.'),
(6, 5, 'Combo Ép Vitamin C Buổi Sáng', 85000, 'Trái cây giàu vitamin C khởi đầu ngày mới: cam sành mọng nước, bưởi da xanh và xoài cát thanh mát.', 'Nước Ép / Sinh Tố', 'BREAKFAST', 'MIEN_TAY', 50, 0, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916481/brands/s5ld5232nl4s4fwczawr.jpg', 'ACTIVE', 'Vắt nước cam hoặc ép bưởi da xanh uống tươi nguyên chất, không cần thêm nguyên liệu bên ngoài.'),
(9, 4, 'Combo Súp Nấm Hạt Sen Dưỡng Thần', 80000, 'Bữa sáng ấm bụng nhẹ nhàng: nấm đùi gà dai ngọt, hạt sen tươi bổ dưỡng, bắp ngọt và cà rốt bùi.', 'Món Canh / Súp', 'BREAKFAST', 'MIEN_BAC', 50, 0, 'https://images.unsplash.com/photo-1547592166-23ac45744acd?w=600&h=400&fit=crop', 'ACTIVE', 'Nấu súp ngọt tự nhiên từ nấm và bắp ngọt; có thể thêm ức gà xé sợi hoặc nấu thuần chay thanh tịnh.'),
(10, 4, 'Combo Canh Nấm Đậu Hũ Thanh Đạm', 75000, 'Bữa sáng ấm nóng: nấm rơm búp, đậu hũ non béo mềm, cải bó xôi và cà rốt giàu protein thực vật.', 'Món Canh', 'BREAKFAST', 'MIEN_TRUNG', 50, 0, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&h=400&fit=crop', 'ACTIVE', 'Món chay hoàn chỉnh giàu đạm thực vật từ đậu hũ non và nấm rơm; rắc hành ngò tiêu xay, không cần thêm thịt cá.'),

-- BỮA TRƯA (LUNCH)
(3, 4, 'Combo Hầm Súp Củ Quả Đà Lạt', 85000, 'Rau củ đầy đủ hầm nồi súp trưa dinh dưỡng: khoai tây ruột vàng, cà rốt hữu cơ, khoai lang và cải bó xôi.', 'Món Kho / Hầm', 'LUNCH', 'DA_LAT', 50, 0, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916676/brands/bmgnazybi0rro8bnxhwp.jpg', 'ACTIVE', 'Hầm lấy nước ngọt tự nhiên từ củ quả Đà Lạt; rất hợp ninh cùng 200g sườn non heo hoặc thịt bắp bò mua ngoài chợ.'),
(7, 5, 'Combo Xào Cà Tím & Canh Rau Ngót', 60000, 'Cơm trưa đậm vị quê nhà miền Tây: bí đỏ hồ lô, củ cải trắng ngọt mát, mồng tơi và rau ngót non.', 'Món Xào & Canh', 'LUNCH', 'MIEN_TAY', 50, 0, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943685/brands/sn8j7wqbpp6uhtitk5nf.jpg', 'ACTIVE', 'Cà tím xào thơm với tía tô hoặc thịt băm; rau ngót nấu canh tôm đồng hoặc thịt nạc dăm ngọt mát.'),
(11, 5, 'Combo Canh Chua Nông Sản Nam Bộ', 70000, 'Canh chua thanh tịnh: cà chua bi chín cây, củ cải trắng thanh ngọt, bắp ngọt Tiền Giang và mồng tơi tươi.', 'Món Canh', 'LUNCH', 'MIEN_NAM', 50, 0, 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600&h=400&fit=crop', 'ACTIVE', 'Rất hợp nấu canh chua cùng cá lóc hoặc tôm sông (mua ngoài chợ); hoặc nấu canh chua chay với đậu hũ non.'),
(12, 4, 'Combo Rau Củ Kho Quẹt & Đậu Hũ', 80000, 'Đậu hũ non organic áp chảo ăn kèm đĩa rau củ hấp ngũ sắc: bông cải xanh, cà rốt và cải ngọt Đà Lạt.', 'Món Hấp / Luộc', 'LUNCH', 'MIEN_NAM', 50, 0, 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=600&h=400&fit=crop', 'ACTIVE', 'Hấp chín ngũ sắc chấm kèm kho quẹt tôm thịt hoặc kho quẹt nấm chay; đậu hũ áp chảo vàng giòn.'),
(13, 4, 'Combo Nấm Xào Ngũ Sắc Dinh Dưỡng', 85000, 'Nấm đùi gà, nấm rơm xào cùng bông cải xanh giòn ngọt và cà rốt giàu chất xơ, đạm lành mạnh.', 'Món Xào', 'LUNCH', 'DA_LAT', 50, 0, 'https://images.unsplash.com/photo-1543339308-43e59d6b73a6?w=600&h=400&fit=crop', 'ACTIVE', 'Nấm và bông cải xào tỏi dầu hào; có thể xào cùng 150g thịt bò nạc thái mỏng hoặc giữ thuần chay ăn thanh đạm.'),
(19, 4, 'Combo Cơm Nhà Đậu Sốt Cà & Rau Luộc', 75000, 'Mâm cơm trưa truyền thống ấm cúng: đậu hũ non sốt cà chua bi, canh rau muống luộc và bí ngòi xanh ngọt mát.', 'Món Mặn & Canh', 'LUNCH', 'MIEN_BAC', 50, 0, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&h=400&fit=crop', 'ACTIVE', 'Mâm cơm thanh đạm đạm thực vật hoàn chỉnh: Đậu hũ sốt cà chua, canh rau muống luộc dầm sấu/chanh, không cần thêm thịt cá.'),

-- BỮA TỐI (DINNER)
(4, 4, 'Combo Xào Củ Quả Thập Cẩm', 75000, 'Món xào tối ấm nóng nhanh gọn: bông cải xanh, cà chua bi mọng nước, khoai lang và cà rốt Đà Lạt.', 'Món Xào', 'DINNER', 'DA_LAT', 50, 0, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917463/brands/xjz74sedvolg7woxywpo.jpg', 'ACTIVE', 'Xào nhanh với tỏi và dầu hào giòn ngọt; có thể xào kèm mực tươi, thịt thăn heo hoặc xào chay.'),
(14, 5, 'Combo Hầm Bí Đỏ Hạt Sen Hạnh Phúc', 85000, 'Món hầm tối bổ dưỡng an thần: bí đỏ hồ lô bùi dẻo, hạt sen tươi Đồng Tháp, cà rốt và nấm đùi gà.', 'Món Kho / Hầm', 'DINNER', 'MIEN_TAY', 50, 0, 'https://images.unsplash.com/photo-1505576399279-565b52d4ac71?w=600&h=400&fit=crop', 'ACTIVE', 'Món canh hầm bổ dưỡng an thần; rất thích hợp ninh cùng 200g sườn sụn heo hoặc hầm chay thanh nhẹ cho bữa tối.'),
(15, 4, 'Combo Lẩu Nấm Thanh Tịnh Gia Đình', 110000, 'Set lẩu tối ấm cúng sum vầy: nấm đùi gà, nấm rơm búp, bắp ngọt, đậu hũ non mềm mịn và cải ngọt non.', 'Món Lẩu', 'DINNER', 'MIEN_BAC', 50, 0, 'https://images.unsplash.com/photo-1547592166-23ac45744acd?w=600&h=400&fit=crop', 'ACTIVE', 'Set lẩu sum họp cuối tuần ngọt nước từ củ quả; có thể nhúng thêm bò cuộn nấm kim châm, tôm tươi hoặc chả nấm chay.'),
(16, 5, 'Combo Rau Củ Hầm Canh Ngọt Lành', 70000, 'Canh tối thanh nhẹ dễ tiêu hóa: khoai lang tím bùi, bắp ngọt vàng, củ cải trắng và mồng tơi xanh.', 'Món Canh', 'DINNER', 'MIEN_TRUNG', 50, 0, 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600&h=400&fit=crop', 'ACTIVE', 'Canh tối thanh ngọt dễ tiêu; hợp nấu cùng 150g tôm sông băm nhỏ hoặc sườn non, giúp ấm bụng ngủ sâu giấc.'),
(20, 4, 'Combo Bí Ngòi Xào Nấm & Canh Mồng Tơi', 75000, 'Bữa tối nhẹ nhàng ít calo: bí ngòi xanh ngọt giòn, nấm đùi gà xào thơm, canh mồng tơi thanh nhiệt.', 'Món Xào & Canh', 'DINNER', 'MIEN_BAC', 50, 0, 'https://images.unsplash.com/photo-1543339308-43e59d6b73a6?w=600&h=400&fit=crop', 'ACTIVE', 'Bí ngòi xào nấm đùi gà thơm ngậy; mồng tơi nấu canh cua đồng hoặc tôm băm ngọt mát thanh nhiệt.'),

-- BỮA PHỤ / TRÁNG MIỆNG (SNACK)
(5, 4, 'Combo Tráng Miệng Trái Cây Đà Lạt', 75000, 'Đĩa trái cây tươi bổ sung năng lượng: dâu tây giống Nhật chua ngọt, chuối già giàu kali và cà chua bi.', 'Tráng Miệng', 'SNACK', 'DA_LAT', 50, 0, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943061/brands/qtspypfmdjcxtsbme7xi.jpg', 'ACTIVE', 'Trái cây tươi Đà Lạt rửa sạch ăn liền trực tiếp, hoặc dầm với sữa chua không đường và hạt granola.'),
(8, 5, 'Combo Giải Nhiệt Mùa Hè', 65000, 'Trái cây giải khát mọng nước miền Tây: dưa hấu ruột đỏ ngọt lịm, cam sành mọng nước và bưởi da xanh.', 'Tráng Miệng', 'SNACK', 'MIEN_TAY', 50, 0, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917373/brands/h0mivuwgipa6hssaawfi.jpg', 'ACTIVE', 'Đĩa trái cây giải khát mọng nước miền Tây ăn tráng miệng sau bữa trưa hoặc ướp lạnh ăn xế chiều.'),
(17, 5, 'Combo Sinh Tố Bơ Sáp Dinh Dưỡng', 65000, 'Bữa phụ giàu chất béo tốt và vitamin: bơ sáp Đắk Lắk dẻo béo, chuối chín thơm và cam sành tươi.', 'Nước Ép / Sinh Tố', 'SNACK', 'MIEN_TRUNG', 50, 0, 'https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=600&h=400&fit=crop', 'ACTIVE', 'Xay sinh tố bơ sáp chuối cùng sữa tươi ít đường và đá nhuyễn, bổ sung chất béo lành mạnh cho bữa phụ.'),
(18, 5, 'Combo Đĩa Trái Cây Miền Tây Tươi Mát', 70000, 'Trái cây đặc sản miền Tây ngọt lịm: bưởi da xanh Bến Tre, dưa hấu đỏ và xoài cát Hòa Lộc thơm nức.', 'Tráng Miệng', 'SNACK', 'MIEN_TAY', 50, 0, 'https://images.unsplash.com/photo-1519996529931-28324d5a630e?w=600&h=400&fit=crop', 'ACTIVE', 'Đĩa trái cây đặc sản miền Tây ngọt lịm, bổ sung vitamin C và chất xơ tự nhiên cho cả gia đình.')
ON DUPLICATE KEY UPDATE combo_name=VALUES(combo_name), price=VALUES(price), type=VALUES(type), meal_type=VALUES(meal_type), region=VALUES(region), description=VALUES(description), cooking_suggestion=VALUES(cooking_suggestion), image_url=VALUES(image_url), status=VALUES(status);

-- ĐỊNH LƯỢNG NGUYÊN LIỆU CHO TỪNG COMBO THEO GRAM (150g - 400g) ĐỂ TÍNH TOÁN CALO & MACRO CHUẨN XÁC
DELETE FROM product_combo WHERE combo_id > 0;

INSERT INTO product_combo (combo_id, product_id, product_quantity) VALUES
-- Combo 1: Detox Củ Quả Sáng (~308 kcal)
(1, 105, 200), (1, 102, 200), (1, 107, 200), (1, 103, 150),
-- Combo 2: Salad Eat Clean Đà Lạt (~151 kcal)
(2, 103, 200), (2, 108, 200), (2, 104, 200), (2, 218, 150),
-- Combo 3: Hầm Súp Củ Quả Đà Lạt (~480 kcal)
(3, 106, 250), (3, 105, 200), (3, 107, 200), (3, 104, 150),
-- Combo 4: Xào Củ Quả Thập Cẩm (~354 kcal)
(4, 214, 250), (4, 108, 200), (4, 107, 200), (4, 105, 150),
-- Combo 5: Tráng Miệng Trái Cây Đà Lạt (~269 kcal)
(5, 109, 200), (5, 216, 200), (5, 108, 150),
-- Combo 6: Ép Vitamin C Buổi Sáng (~375 kcal)
(6, 206, 300), (6, 205, 300), (6, 207, 200),
-- Combo 7: Xào Cà Tím & Canh Rau Ngót (~190 kcal)
(7, 203, 300), (7, 204, 200), (7, 202, 200), (7, 201, 150),
-- Combo 8: Giải Nhiệt Mùa Hè (~332 kcal)
(8, 208, 400), (8, 206, 250), (8, 205, 250),
-- Combo 9: Súp Nấm Hạt Sen Dưỡng Thần (~481 kcal)
(9, 209, 200), (9, 212, 200), (9, 213, 200), (9, 105, 150),
-- Combo 10: Canh Nấm Đậu Hũ Thanh Đạm (~361 kcal)
(10, 210, 200), (10, 211, 250), (10, 104, 200), (10, 105, 150),
-- Combo 11: Canh Chua Nông Sản Nam Bộ (~268 kcal)
(11, 108, 200), (11, 204, 200), (11, 213, 200), (11, 202, 200),
-- Combo 12: Rau Củ Kho Quẹt & Đậu Hũ (~343 kcal)
(12, 211, 250), (12, 214, 200), (12, 105, 150), (12, 102, 150),
-- Combo 13: Nấm Xào Ngũ Sắc Dinh Dưỡng (~263 kcal)
(13, 209, 200), (13, 210, 200), (13, 214, 200), (13, 105, 150),
-- Combo 14: Hầm Bí Đỏ Hạt Sen Hạnh Phúc (~369 kcal)
(14, 203, 300), (14, 212, 200), (14, 105, 150), (14, 209, 150),
-- Combo 15: Lẩu Nấm Thanh Tịnh Gia Đình (~545 kcal)
(15, 209, 250), (15, 210, 200), (15, 213, 200), (15, 211, 250), (15, 102, 200),
-- Combo 16: Rau Củ Hầm Canh Ngọt Lành (~440 kcal)
(16, 107, 250), (16, 213, 200), (16, 204, 200), (16, 202, 150),
-- Combo 17: Sinh Tố Bơ Sáp Dinh Dưỡng (~523 kcal)
(17, 215, 200), (17, 216, 150), (17, 206, 150),
-- Combo 18: Đĩa Trái Cây Miền Tây Tươi Mát (~324 kcal)
(18, 205, 300), (18, 208, 300), (18, 207, 200),
-- Combo 19: Cơm Nhà Đậu Sốt Cà & Rau Luộc (~298 kcal)
(19, 211, 250), (19, 108, 200), (19, 101, 200), (19, 217, 200),
-- Combo 20: Bí Ngòi Xào Nấm & Canh Mồng Tơi (~201 kcal)
(20, 217, 250), (20, 209, 200), (20, 202, 200), (20, 105, 150)
ON DUPLICATE KEY UPDATE product_quantity=VALUES(product_quantity);

-- 3.6 PRODUCT IMAGES
INSERT INTO product_images (product_id, image_url, is_primary, display_order) VALUES
(101, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943166/brands/ezrip6hvips9nwmyottf.jpg', 1, 1),
(102, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943139/brands/mhfnqgigq3xwxivi3r0u.jpg', 1, 1),
(103, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916585/brands/ojx1comtiw3wbpq4rlzy.jpg', 1, 1),
(104, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249130/brands/vleciprlqifxmkwsmlxz.webp', 1, 1),
(105, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916721/brands/uj1eoinkwvthfjvtpvpq.jpg', 1, 1),
(106, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916676/brands/bmgnazybi0rro8bnxhwp.jpg', 1, 1),
(107, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249499/brands/pgdvlccom3lid5t24pnk.jpg', 1, 1),
(108, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916530/brands/xjiiaehe5evwybwhmpmu.jpg', 1, 1),
(109, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943061/brands/qtspypfmdjcxtsbme7xi.jpg', 1, 1),
(201, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250103/brands/qlsbgtgno3kvconpo0c3.jpg', 1, 1),
(202, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250212/brands/mtbfbygr3tmlctmfdg0x.jpg', 1, 1),
(203, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917463/brands/xjz74sedvolg7woxywpo.jpg', 1, 1),
(204, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943219/brands/dysl261o6o2bapfwurmf.jpg', 1, 1),
(205, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917344/brands/c2i9gkbkl8vx7iwulwsz.png', 1, 1),
(206, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916481/brands/s5ld5232nl4s4fwczawr.jpg', 1, 1),
(207, 'https://images.unsplash.com/photo-1553279768-865429fa0078?w=600&h=400&fit=crop', 1, 1),
(208, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917373/brands/h0mivuwgipa6hssaawfi.jpg', 1, 1),
(209, 'https://images.unsplash.com/photo-1504544750208-dc0358e63f7f?w=600&h=400&fit=crop', 1, 1),
(210, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&h=400&fit=crop', 1, 1),
(211, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&h=400&fit=crop', 1, 1),
(212, 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600&h=400&fit=crop', 1, 1),
(213, 'https://images.unsplash.com/photo-1551754655-cd27e38d2076?w=600&h=400&fit=crop', 1, 1),
(214, 'https://images.unsplash.com/photo-1459411621453-7b03977f4bfc?w=600&h=400&fit=crop', 1, 1),
(215, 'https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=600&h=400&fit=crop', 1, 1),
(216, 'https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=600&h=400&fit=crop', 1, 1),
(217, 'https://images.unsplash.com/photo-1582515073490-39981397c445?w=600&h=400&fit=crop', 1, 1),
(218, 'https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?w=600&h=400&fit=crop', 1, 1)
ON DUPLICATE KEY UPDATE is_primary=VALUES(is_primary);

-- 3.6 NUTRITION INFO (ĐẦY ĐỦ 100% CHO 22 SẢN PHẨM TRÊN 100g)
INSERT INTO nutrition_info (product_id, calories, protein_g, carbs_g, fat_g) VALUES
(101, 19.0, 2.6, 3.1, 0.2),  -- Rau muống
(102, 16.0, 1.5, 2.8, 0.2),  -- Cải ngọt
(103, 15.0, 1.4, 2.8, 0.2),  -- Xà lách mỡ
(104, 23.0, 2.9, 3.6, 0.4),  -- Cải bó xôi (Spinach)
(105, 41.0, 0.9, 9.6, 0.2),  -- Cà rốt Đà Lạt
(106, 77.0, 2.0, 17.5, 0.1), -- Khoai tây vàng
(107, 86.0, 1.6, 20.1, 0.1), -- Khoai lang mật tím
(108, 18.0, 0.9, 3.9, 0.2),  -- Cà chua bi
(109, 32.0, 0.7, 7.7, 0.3),  -- Dâu tây giống Nhật
(201, 35.0, 5.3, 3.4, 0.5),  -- Rau ngót hữu cơ
(202, 14.0, 1.9, 2.7, 0.2),  -- Mồng tơi ngọn non
(203, 26.0, 1.0, 6.5, 0.1),  -- Bí đỏ hồ lô
(204, 16.0, 0.7, 3.4, 0.1),  -- Củ cải trắng
(205, 38.0, 0.8, 9.6, 0.1),  -- Bưởi da xanh
(206, 47.0, 0.9, 11.8, 0.1), -- Cam sành Tiền Giang
(207, 60.0, 0.8, 15.0, 0.4), -- Xoài cát Hòa Lộc
(208, 30.0, 0.6, 7.6, 0.2),  -- Dưa hấu ruột đỏ
(209, 35.0, 3.3, 5.0, 0.4),  -- Nấm đùi gà
(210, 32.0, 3.8, 4.5, 0.5),  -- Nấm rơm
(211, 76.0, 8.1, 1.9, 4.8),  -- Đậu hũ non
(212, 89.0, 4.1, 17.2, 0.5), -- Hạt sen tươi
(213, 86.0, 3.2, 19.0, 1.2), -- Bắp ngọt
(214, 34.0, 2.8, 7.0, 0.4),  -- Bông cải xanh
(215, 160.0, 2.0, 8.5, 14.7),-- Bơ sáp
(216, 89.0, 1.1, 22.8, 0.3), -- Chuối già
(217, 17.0, 1.2, 3.1, 0.3),  -- Bí ngòi xanh
(218, 26.0, 1.0, 6.0, 0.3)   -- Ớt chuông đỏ
ON DUPLICATE KEY UPDATE calories=VALUES(calories), protein_g=VALUES(protein_g), carbs_g=VALUES(carbs_g), fat_g=VALUES(fat_g);

-- 3.7 VOUCHERS (ĐỦ CÁC LOẠI: TOÀN SÀN, FREESHIP, SHOP, %, CỐ ĐỊNH)
INSERT INTO vouchers (voucher_id, shop_id, voucher_code, discount_value, max_discount, min_order_value, usage_limit, expiry_date, voucher_type, voucher_scope, voucher_target, claim_limit, claimed_count, used_count, per_user_limit, active) VALUES
(1, 4, 'CAPNONG10K', 10000, 10000, 50000, 100, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FIXED_AMOUNT', 'SHOP', 'ORDER', 100, 1, 0, 1, 1),
(2, 4, 'DALATFREESHIP', 15000, 15000, 100000, 50, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FREE_SHIPPING', 'SHOP', 'SHIPPING', 50, 1, 0, 1, 1),
(3, 5, 'MIENTAY15K', 15000, 15000, 80000, 100, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FIXED_AMOUNT', 'SHOP', 'ORDER', 100, 1, 0, 1, 1),
(4, 4, 'CAPNONG20K', 20000, 20000, 120000, 50, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FIXED_AMOUNT', 'SHOP', 'ORDER', 50, 0, 0, 1, 1),
(5, 1, 'FREESHIPALL', 30000, NULL, 100000, 200, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FREE_SHIPPING', 'PLATFORM', 'SHIPPING', 200, 2, 0, 1, 1),
(6, 1, 'PLATFORM15K', 15000, 15000, 150000, 100, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FIXED_AMOUNT', 'PLATFORM', 'ORDER', 100, 1, 0, 1, 1),
(7, 1, 'PLATFORM10PERCENT', 10, 25000, 100000, 150, DATE_ADD(NOW(), INTERVAL 30 DAY), 'PERCENTAGE', 'PLATFORM', 'ORDER', 150, 1, 0, 1, 1),
(8, 1, 'FREESHIP50K', 50000, NULL, 200000, 50, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FREE_SHIPPING', 'PLATFORM', 'SHIPPING', 50, 0, 0, 1, 1)
ON DUPLICATE KEY UPDATE voucher_code=VALUES(voucher_code);

-- 3.8 VOUCHER USERS (VÍ VOUCHER NGƯỜI DÙNG - BUYER 1 & 2 ĐÃ SỞ HỮU ĐỂ TEST CHECKOUT)
INSERT INTO voucher_users (id, user_id, voucher_id, quantity, quantity_used, claim_at) VALUES
(1, 2, 1, 1, 0, NOW()),  -- Buyer 1 (Sang) có CAPNONG10K
(2, 2, 2, 1, 0, NOW()),  -- Buyer 1 có DALATFREESHIP
(3, 2, 3, 1, 0, NOW()),  -- Buyer 1 có MIENTAY15K
(4, 2, 5, 1, 0, NOW()),  -- Buyer 1 có FREESHIPALL
(5, 2, 6, 1, 0, NOW()),  -- Buyer 1 có PLATFORM15K
(6, 2, 7, 1, 0, NOW()),  -- Buyer 1 có PLATFORM10PERCENT
(7, 3, 1, 1, 0, NOW()),  -- Buyer 2 (Mai) có CAPNONG10K
(8, 3, 5, 1, 0, NOW())   -- Buyer 2 có FREESHIPALL
ON DUPLICATE KEY UPDATE quantity=VALUES(quantity);

-- 3.9 CARTS & SAMPLE CART ITEMS (FOR BUYER 1 & 2)
INSERT INTO carts (id, user_id) VALUES
(1, 2),
(2, 3)
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

INSERT INTO cart_items (cart_id, product_id, mystery_box_id, build_combo_id, quantity) VALUES
(1, 105, NULL, NULL, 1),  -- Cà rốt Đà Lạt hữu cơ (Shop 4)
(1, NULL, NULL, 1, 1),     -- Combo Detox Củ Quả Sáng (Shop 4)
(1, NULL, NULL, 8, 1);     -- Combo Giải Nhiệt Mùa Hè (Shop 5)

-- 3.10 CHECKOUT BATCHES (PHỤC VỤ TEST PAYOS MULTI-ORDER)
INSERT INTO checkout_batches (id, buyer_id, payos_order_code, payment_method, status, total_amount, batch_discount_amount, checkout_url, qr_code_url, created_at) VALUES
(1, 2, '1791793001', 'PAYOS', 'PENDING', 197000, 10000, 'https://pay.payos.vn/web/test1791793001', 'https://qr.payos.vn/test1791793001.png', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(2, 2, '1791793002', 'PAYOS', 'COMPLETED', 167000, 40000, 'https://pay.payos.vn/web/test1791793002', 'https://qr.payos.vn/test1791793002.png', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE payos_order_code=VALUES(payos_order_code);

-- 3.11 ORDERS (ĐA DẠNG TRẠNG THÁI ĐỂ TEST CẢ 3 ROLES: BUYER, SHOP, SHIPPER)
INSERT INTO orders (order_id, buyer_id, shop_id, shipper_id, checkout_batch_id, shipping_fee, recipient_name, recipient_phone, shipping_address, note, status, total_amount, payment_method, shipping_discount_amount, batch_discount_amount, shipping_latitude, shipping_longitude, shop_latitude, shop_longitude, created_at) VALUES
-- Đơn 101: Trong Batch 1, trạng thái AWAITING_PAYMENT (Shop Đà Lạt) -> Test Resume Payment / Split Payment
(101, 2, 4, NULL, 1, 15000, 'Ngô Gia Sang (Khách mua)', '0911111111', '268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh', 'Giao rau củ tươi trong giờ hành chính', 'AWAITING_PAYMENT', 100000, 'PAYOS', 0, 10000, 10.7725, 106.6579, 10.7801, 106.6990, DATE_SUB(NOW(), INTERVAL 2 HOUR)),

-- Đơn 102: Trong Batch 1, trạng thái AWAITING_PAYMENT (Shop Miền Tây)
(102, 2, 5, NULL, 1, 22000, 'Ngô Gia Sang (Khách mua)', '0911111111', '268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh', 'Giao nhẹ tay giúp mình', 'AWAITING_PAYMENT', 97000, 'PAYOS', 0, 0, 10.7725, 106.6579, 10.7850, 106.7100, DATE_SUB(NOW(), INTERVAL 2 HOUR)),

-- Đơn 103: Đơn COD mới tạo (PENDING) -> Test Shop xác nhận đơn
(103, 2, 4, NULL, NULL, 15000, 'Ngô Gia Sang (Khách mua)', '0911111111', '268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh', 'Giao buổi sáng', 'PENDING', 85000, 'COD', 0, 0, 10.7725, 106.6579, 10.7801, 106.6990, DATE_SUB(NOW(), INTERVAL 1 HOUR)),

-- Đơn 104: Đơn đã xác nhận (CONFIRMED) -> Test Shop chuẩn bị hàng
(104, 2, 4, NULL, NULL, 15000, 'Ngô Gia Sang (Khách mua)', '0911111111', '268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh', 'Đóng gói kỹ', 'CONFIRMED', 120000, 'COD', 0, 0, 10.7725, 106.6579, 10.7801, 106.6990, DATE_SUB(NOW(), INTERVAL 3 HOUR)),

-- Đơn 105: Đang giao hàng (SHIPPING) đã gán Shipper Hoàng (id: 6) -> TEST GOOGLE MAPS REALTIME TRACKING
(105, 2, 4, 6, 2, 15000, 'Ngô Gia Sang (Khách mua)', '0911111111', '268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh', 'Gọi trước khi đến 10 phút', 'SHIPPING', 95000, 'PAYOS', 15000, 10000, 10.7725, 106.6579, 10.7801, 106.6990, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),

-- Đơn 106: Đã giao thành công (DELIVERED) -> Test Đánh giá / Hoàn thành
(106, 2, 5, 6, NULL, 15000, 'Ngô Gia Sang (Khách mua)', '0911111111', '268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh', 'Giao hàng thành công', 'DELIVERED', 135000, 'COD', 0, 15000, 10.7725, 106.6579, 10.7850, 106.7100, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- Đơn 107: Đã hủy (CANCELLED)
(107, 2, 4, NULL, NULL, 15000, 'Ngô Gia Sang (Khách mua)', '0911111111', '268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh', 'Khách đổi ý', 'CANCELLED', 50000, 'COD', 0, 0, 10.7725, 106.6579, 10.7801, 106.6990, DATE_SUB(NOW(), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- 3.12 ORDER DETAILS (CHI TIẾT MẶT HÀNG TỪNG ĐƠN: SẢN PHẨM, TÚI MÙ, COMBO)
INSERT INTO order_details (order_detail_id, order_id, product_id, mystery_id, combo_id, quantity, unit_price, item_type) VALUES
(1, 101, 105, NULL, NULL, 1, 25000, 'PRODUCT'),        -- Cà rốt Đà Lạt (Đơn 101)
(2, 101, NULL, NULL, 1, 1, 70000, 'BUILD_COMBO'),     -- Combo Detox Củ Quả Sáng (Đơn 101)
(3, 102, NULL, NULL, 8, 1, 75000, 'BUILD_COMBO'),     -- Combo Giải Nhiệt Mùa Hè (Đơn 102)
(4, 103, 101, NULL, NULL, 2, 12000, 'PRODUCT'),        -- Rau muống (Đơn 103)
(5, 103, 106, NULL, NULL, 1, 30000, 'PRODUCT'),        -- Khoai tây vàng (Đơn 103)
(6, 104, NULL, 1, NULL, 1, 89000, 'MYSTERY_BOX'),     -- Hộp Củ Bí Ẩn Đà Lạt (Đơn 104)
(7, 105, 105, NULL, NULL, 1, 25000, 'PRODUCT'),        -- Cà rốt (Đơn 105 đang giao)
(8, 105, NULL, NULL, 1, 1, 70000, 'BUILD_COMBO'),     -- Combo Detox (Đơn 105 đang giao)
(9, 106, 205, NULL, NULL, 2, 65000, 'PRODUCT')         -- Bưởi da xanh (Đơn 106 đã giao)
ON DUPLICATE KEY UPDATE quantity=VALUES(quantity);

-- 3.13 PAYMENTS (LIÊN KẾT THANH TOÁN VỚI ĐƠN HÀNG)
INSERT INTO payments (payment_id, order_id, payment_date, amount, payment_gateway, status, payos_order_code, checkout_url, qr_code_url, created_at) VALUES
(1, 101, NULL, 197000, 'PAYOS', 'PENDING', '1791793001', 'https://pay.payos.vn/web/test1791793001', 'https://qr.payos.vn/test1791793001.png', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(2, 105, DATE_SUB(NOW(), INTERVAL 35 MINUTE), 95000, 'PAYOS', 'PAID', '1791793002', 'https://pay.payos.vn/web/test1791793002', 'https://qr.payos.vn/test1791793002.png', DATE_SUB(NOW(), INTERVAL 40 MINUTE))
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- 3.14 VOUCHER USAGES (LỊCH SỬ SỬ DỤNG MÃ)
INSERT INTO voucher_usages (id, order_id, user_id, voucher_id, discount_amount, used_at) VALUES
(1, 105, 2, 1, 10000, DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(2, 105, 2, 5, 15000, DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(3, 106, 2, 3, 15000, DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE discount_amount=VALUES(discount_amount);

-- 3.15 SHIPPER LOCATIONS (VỊ TRÍ GPS HIỆN TẠI PHỤC VỤ THEO DÕI LIVE GOOGLE MAPS)
INSERT INTO shipper_locations (id, order_id, shipper_id, latitude, longitude, updated_at) VALUES
(1, 105, 6, 10.7769, 106.7009, NOW())
ON DUPLICATE KEY UPDATE latitude=VALUES(latitude), longitude=VALUES(longitude);

-- 3.9 WALLETS
INSERT INTO wallets (user_id, status, type, total_balance, frozen_balance, total_revenue_all_time, total_withdrawn, commission_wallet) VALUES
(1, 'ACTIVE', 'PLATFORM', 100000000, 0, 0, 0, 50000),
(4, 'ACTIVE', 'SHOP', 1500000, 0, 1500000, 0, 45000),
(5, 'ACTIVE', 'SHOP', 1200000, 0, 1200000, 0, 36000),
(6, 'ACTIVE', 'SHIPPER', 250000, 0, 250000, 0, 0)
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

-- 3.10 ALLERGENS & HEALTH CONDITIONS
INSERT INTO allergens (id, name) VALUES
(1, 'Đậu phộng / Lạc'), (2, 'Phấn hoa thực vật'), (3, 'Đậu nành'), (4, 'Gluten'),
(5, 'Hải sản / Tôm cua'), (6, 'Trứng gà'), (7, 'Sữa bò / Lactose'), (8, 'Mè / Vừng'),
(9, 'Cà rốt'), (10, 'Dưa leo'), (11, 'Nấm các loại'), (12, 'Cá biển')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO health_conditions (id, name) VALUES
(1, 'Tiểu đường type 2 (Cần giảm đường bột)'),
(2, 'Huyết áp cao (Ăn nhạt, nhiều kali)'),
(3, 'Đau dạ dày / Tiêu hóa nhạy cảm'),
(4, 'Ăn kiêng giảm cân (Keto / Low-carb)'),
(5, 'Trào ngược dạ dày thực quản (GERD)'),
(6, 'Gout / Tăng acid uric máu'),
(7, 'Mỡ máu cao / Tim mạch'),
(8, 'Suy thận nhẹ (Hạn chế kali, đạm cao)'),
(9, 'Viêm xoang / Hạn chế đồ lạnh'),
(10, 'Hội chứng ruột kích thích (IBS)')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Gán dị ứng cho sản phẩm: Đậu hũ non organic (211) chứa Đậu nành (3)
INSERT INTO product_allergens (product_id, allergen_id) VALUES
(211, 3)
ON DUPLICATE KEY UPDATE allergen_id=VALUES(allergen_id);

-- Dị ứng & tình trạng sức khỏe mẫu cho người dùng để test AI Plan
INSERT INTO user_allergies (buyer_id, allergen_id) VALUES
(2, 3), -- Buyer 1 (Sang): Dị ứng Đậu nành
(2, 1), -- Buyer 1 (Sang): Dị ứng Đậu phộng / Lạc
(3, 1)  -- Buyer 2 (Mai): Dị ứng Đậu phộng / Lạc
ON DUPLICATE KEY UPDATE allergen_id=VALUES(allergen_id);

INSERT INTO user_health_conditions (buyer_id, condition_id) VALUES
(2, 3), -- Buyer 1 (Sang): Đau dạ dày / Tiêu hóa nhạy cảm
(2, 2), -- Buyer 1 (Sang): Huyết áp cao (Ăn nhạt, nhiều kali)
(3, 4)  -- Buyer 2 (Mai): Ăn kiêng giảm cân (Keto / Low-carb)
ON DUPLICATE KEY UPDATE condition_id=VALUES(condition_id);

-- Lịch sử trích xuất hồ sơ sức khỏe bằng AI (Profile Update Requests)
INSERT INTO profile_update_requests (id, buyer_id, raw_input_text, extracted_diff, confirmed_at, created_at) VALUES
(1, 2, 'Tôi bị đau dạ dày, huyết áp cao và dị ứng với đậu phộng, đậu nành', '{"add_conditions":["Đau dạ dày / Tiêu hóa nhạy cảm","Huyết áp cao (Ăn nhạt, nhiều kali)"],"add_allergies":["Đậu nành","Đậu phộng / Lạc"],"diet_preference":"VEGETARIAN"}', NOW(), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 3, 'Tôi muốn ăn kiêng giảm cân low-carb, không ăn được đậu phộng', '{"add_conditions":["Ăn kiêng giảm cân (Keto / Low-carb)"],"add_allergies":["Đậu phộng / Lạc"],"diet_preference":"VEGAN"}', NOW(), DATE_SUB(NOW(), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE buyer_id=VALUES(buyer_id);

-- ==============================================================================
-- 3.10b SEED DỮ LIỆU BUILD PLANS, PLAN DAYS, PLAN MEALS & PLAN MEAL ITEMS
-- Phục vụ kiểm thử toàn diện luồng Thực đơn dinh dưỡng & AI Co-pilot:
-- - Plan 1: AI_DRAFT (Kế hoạch 3 ngày đang nháp, test AI Copilot chat & action cards đổi món)
-- - Plan 2: CONFIRMED (Kế hoạch 7 ngày đã chốt, test tracking hoàn thành bữa ăn & shopping list)
-- - Plan 3: ARCHIVED (Kế hoạch 5 ngày đã lưu trữ, test quyền hạn chỉ đọc)
-- ==============================================================================

INSERT INTO build_plans (plan_id, buyer_id, plan_name, number_of_days, number_of_people, meal_type, target_budget, status, source, created_at, updated_at) VALUES
(1, 2, 'Thực Đơn Ăn Chay Thanh Lọc 3 Ngày (AI Draft)', 3, 2, 'BREAKFAST,LUNCH,DINNER', 750000, 'AI_DRAFT', 'AI', NOW(), NOW()),
(2, 2, 'Kế Hoạch Dinh Dưỡng Eat Clean 7 Ngày (Đã Xác Nhận)', 7, 3, 'LUNCH,DINNER', 1150000, 'CONFIRMED', 'AI', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(3, 3, 'Thực Đơn Giảm Cân Tuần Trước (Lưu Trữ)', 5, 1, 'LUNCH,DINNER', 650000, 'ARCHIVED', 'AI', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE plan_name=VALUES(plan_name), status=VALUES(status);

-- PLAN 1: 3 NGÀY (DAYS 1 - 3)
INSERT INTO plan_days (id, plan_id, day_index, date) VALUES
(1, 1, 1, CURRENT_DATE),
(2, 1, 2, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY)),
(3, 1, 3, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE day_index=VALUES(day_index);

-- PLAN 1: MEALS (3 BỮA/NGÀY = 9 MEALS)
INSERT INTO plan_meals (id, plan_day_id, meal_type, sort_order, completed) VALUES
(1, 1, 'BREAKFAST', 1, 0),
(2, 1, 'LUNCH', 2, 0),
(3, 1, 'DINNER', 3, 0),
(4, 2, 'BREAKFAST', 1, 0),
(5, 2, 'LUNCH', 2, 0),
(6, 2, 'DINNER', 3, 0),
(7, 3, 'BREAKFAST', 1, 0),
(8, 3, 'LUNCH', 2, 0),
(9, 3, 'DINNER', 3, 0)
ON DUPLICATE KEY UPDATE meal_type=VALUES(meal_type);

-- PLAN 1: MEAL ITEMS (GÁN CÁC COMBO THỰC TẾ)
INSERT INTO plan_meal_items (id, plan_meal_id, combo_id, quantity, unit_price_snapshot, is_ai_suggested, position, user_override) VALUES
-- Ngày 1: Sáng (Detox Củ Quả 70k), Trưa (Hầm Súp Củ Quả 85k), Tối (Xào Củ Quả 75k)
(1, 1, 1, 1, 70000, 1, 1, 0),
(2, 2, 3, 1, 85000, 1, 1, 0),
(3, 3, 4, 1, 75000, 1, 1, 0),
-- Ngày 2: Sáng (Salad Eat Clean 75k), Trưa (Canh Chua Nam Bộ 70k), Tối (Hầm Bí Đỏ Hạt Sen 85k)
(4, 4, 2, 1, 75000, 1, 1, 0),
(5, 5, 11, 1, 70000, 1, 1, 0),
(6, 6, 14, 1, 85000, 1, 1, 0),
-- Ngày 3: Sáng (Súp Nấm Hạt Sen 80k), Trưa (Nấm Xào Ngũ Sắc 85k), Tối (Rau Củ Hầm Canh 70k)
(7, 7, 9, 1, 80000, 1, 1, 0),
(8, 8, 13, 1, 85000, 1, 1, 0),
(9, 9, 16, 1, 70000, 1, 1, 0)
ON DUPLICATE KEY UPDATE combo_id=VALUES(combo_id);

-- PLAN 2: 7 NGÀY (DAYS 4 - 10)
INSERT INTO plan_days (id, plan_id, day_index, date) VALUES
(4, 2, 1, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY)),
(5, 2, 2, CURRENT_DATE),
(6, 2, 3, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY)),
(7, 2, 4, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY)),
(8, 2, 5, DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY)),
(9, 2, 6, DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY)),
(10, 2, 7, DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE day_index=VALUES(day_index);

-- PLAN 2: MEALS (2 BỮA/NGÀY = 14 MEALS)
INSERT INTO plan_meals (id, plan_day_id, meal_type, sort_order, completed) VALUES
(10, 4, 'LUNCH', 1, 1), -- Đã ăn
(11, 4, 'DINNER', 2, 1), -- Đã ăn
(12, 5, 'LUNCH', 1, 1), -- Đã ăn
(13, 5, 'DINNER', 2, 0),
(14, 6, 'LUNCH', 1, 0),
(15, 6, 'DINNER', 2, 0),
(16, 7, 'LUNCH', 1, 0),
(17, 7, 'DINNER', 2, 0),
(18, 8, 'LUNCH', 1, 0),
(19, 8, 'DINNER', 2, 0),
(20, 9, 'LUNCH', 1, 0),
(21, 9, 'DINNER', 2, 0),
(22, 10, 'LUNCH', 1, 0),
(23, 10, 'DINNER', 2, 0)
ON DUPLICATE KEY UPDATE meal_type=VALUES(meal_type);

-- PLAN 2: MEAL ITEMS
INSERT INTO plan_meal_items (id, plan_meal_id, combo_id, quantity, unit_price_snapshot, is_ai_suggested, position, user_override) VALUES
(10, 10, 3, 1, 85000, 1, 1, 0),
(11, 11, 4, 1, 75000, 1, 1, 0),
(12, 12, 11, 1, 70000, 1, 1, 0),
(13, 13, 14, 1, 85000, 1, 1, 0),
(14, 14, 12, 1, 80000, 1, 1, 0),
(15, 15, 15, 1, 110000, 1, 1, 0),
(16, 16, 13, 1, 85000, 1, 1, 0),
(17, 17, 16, 1, 70000, 1, 1, 0),
(18, 18, 7, 1, 60000, 1, 1, 0),
(19, 19, 20, 1, 75000, 1, 1, 0),
(20, 20, 19, 1, 75000, 1, 1, 0),
(21, 21, 4, 1, 75000, 1, 1, 0),
(22, 22, 11, 1, 70000, 1, 1, 0),
(23, 23, 14, 1, 85000, 1, 1, 0)
ON DUPLICATE KEY UPDATE combo_id=VALUES(combo_id);

-- PLAN GENERATION REQUEST (LOG TRUY VẾT AI)
INSERT INTO plan_generation_requests (id, plan_id, buyer_id, requested_at, input_params, raw_ai_response) VALUES
(1, 1, 2, NOW(), '{"goal":"VEGAN","numberOfDays":3,"numberOfPeople":2,"targetBudget":750000,"preferredMealTypes":["BREAKFAST","LUNCH","DINNER"]}', '{"ai_rationale":"Thực đơn thuần chay 3 ngày giàu chất xơ từ nấm và củ quả tươi, hoàn toàn không có đậu nành và đậu phộng phù hợp hồ sơ sức khỏe."}'),
(2, 2, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), '{"goal":"HEALTHY","numberOfDays":7,"numberOfPeople":3,"targetBudget":1150000,"preferredMealTypes":["LUNCH","DINNER"]}', '{"ai_rationale":"Thực đơn 7 ngày Eat Clean cân đối calo, luân phiên canh và món xào ngũ sắc giàu vitamin."}')
ON DUPLICATE KEY UPDATE plan_id=VALUES(plan_id);

-- 3.11 BOT KNOWLEDGE
INSERT INTO bot_knowledge (id, type, title, content, keywords, active) VALUES
(1, 'SHIPPING', 'Chính sách vận chuyển CapNong', 'CapNong liên kết tài xế giao hàng siêu tốc trong 2-4 giờ từ nông trại/nhà vườn trực tiếp đến tay người tiêu dùng, bảo quản trong thùng giữ lạnh.', 'giao hàng, vận chuyển, phí ship, thời gian giao', 1),
(2, 'QUALITY', 'Tiêu chuẩn chất lượng nông sản', 'Mỗi lô nông sản được kiểm định bởi chuyên gia và hệ thống AI đánh giá độ tươi (Freshness Score) trước khi lên kệ.', 'chất lượng, độ tươi, vietgap, hữu cơ, an toàn', 1),
(3, 'ORDER', 'Hình thức thanh toán', 'CapNong hỗ trợ thanh toán tiền mặt khi nhận hàng (COD) và Chuyển khoản trực tuyến quét mã VietQR PayOS tức thì.', 'thanh toán, payos, qr, cod, chuyển khoản', 1)
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- 3.12 SAVED ADDRESSES (SỔ ĐỊA CHỈ NHẬN HÀNG)
INSERT INTO saved_addresses (id, user_id, label, recipient_name, phone, address, latitude, longitude, is_default, is_active) VALUES
(1, 2, 'Nhà', 'Ngô Gia Sang (Khách mua)', '0911111111', '268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh', 10.7725, 106.6579, 1, 1),
(2, 2, 'Văn phòng', 'Ngô Gia Sang (Khách mua)', '0911111111', 'Chợ Bến Thành, Đường Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh', 10.7721, 106.6983, 0, 1),
(3, 3, 'Nhà', 'Trần Thị Mai', '0911111112', 'Chợ Bến Thành, Đường Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh', 10.7721, 106.6983, 1, 1)
ON DUPLICATE KEY UPDATE address=VALUES(address), recipient_name=VALUES(recipient_name);

SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================================
-- HOÀN TẤT KHỞI TẠO DỮ LIỆU THÀNH CÔNG (DB_CapNong)
-- Sẵn sàng chạy kiểm thử từ Cart -> Checkout -> Order (PENDING/PAID) -> Payment PayOS!
-- ==============================================================================
