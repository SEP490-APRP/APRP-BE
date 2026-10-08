-- ==============================================================================
-- CAPNONG (SEP490) - FULL DATABASE SCHEMA & SEED DATA (sep.sql)
-- Chuẩn hóa theo 100% Entity Spring Boot (APRP-BE)
-- Tham khảo cấu trúc và dữ liệu từ swd.sql và exe.sql
-- Mật khẩu mặc định tất cả tài khoản test: Admin@123 (hoặc 123456)
-- BCrypt hash: $2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K (Admin@123)
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS DB_CapNong CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE DB_CapNong;

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
    status ENUM('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'ACTIVE',
    source ENUM('AI_GENERATED', 'MANUAL') DEFAULT 'AI_GENERATED',
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
INSERT INTO users (id, email, password, role_id, status, full_name, phone_number, address, shop_name, shop_active, bank_name, bank_account, bank_account_holder, rating_average, description, logo_url, achievement, license, vehicle_number, create_at) VALUES
(1, 'admin@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 4, 'ACTIVE', 'Quản Trị Viên Hệ Thống', '0900000000', '1 Công Xã Paris, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh', NULL, 0, 'Vietcombank', '0011001234567', 'CAPNONG ADMIN', 5.0, 'Quản trị viên toàn hệ thống CapNong', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', 'SuperAdmin', NULL, NULL, NOW()),

(2, 'buyer1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 1, 'ACTIVE', 'Ngô Gia Sang (Khách mua)', '0911111111', '268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh', NULL, 0, 'VietinBank', '101888888888', 'NGO GIA SANG', 5.0, 'Khách hàng thân thiết', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', NULL, NULL, NULL, NOW()),

(3, 'buyer2@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 1, 'ACTIVE', 'Trần Thị Mai', '0911111112', 'Chợ Bến Thành, Đường Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh', NULL, 0, 'MBBank', '970422888888', 'TRAN THI MAI', 5.0, 'Khách hàng', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', NULL, NULL, NULL, NOW()),

(4, 'shop1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 2, 'ACTIVE', 'Trần Văn Kiệt (Nhà Vườn 1)', '0933333333', 'Số 1 Võ Văn Ngân, Phường Linh Chiểu, Thành phố Thủ Đức, TP. Hồ Chí Minh', 'Nông Sản Xấu Mã - Vườn Đà Lạt', 1, 'Vietcombank', '0071000123456', 'TRAN VAN KIET', 4.9, 'Chuyên rau củ quả organic sạch từ Đà Lạt và các vùng nông nghiệp sạch chuẩn VietGAP.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252594/brands/srflhqfol1ckwavsaa1n.jpg', 'Hạt Giống Vàng', NULL, NULL, NOW()),

(5, 'shop2@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 2, 'ACTIVE', 'Lê Thị Ngọc (Nhà Vườn 2)', '0944444444', 'Đường Nguyễn Du, Khu Đô thị Đại học Quốc gia, Dĩ An, Bình Dương', 'Vườn Xanh Bình Dương & Miền Tây', 1, 'Techcombank', '19034567890123', 'LE THI NGOC', 4.8, 'Chuyên rau củ quả tươi miền Nam, trái cây đặc sản miền Tây chất lượng cao, thu hái trong ngày.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252337/brands/t9muel0yqmqfzfu3ndij.jpg', 'Nông Dân Tinh Hoa', NULL, NULL, NOW()),

(6, 'shipper1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 3, 'ACTIVE', 'Nguyễn Văn Hoàng (Shipper)', '0922222222', 'Số 1 Võ Văn Ngân, Phường Linh Chiểu, Thành phố Thủ Đức, TP. Hồ Chí Minh', NULL, 0, 'VietinBank', '101899999999', 'NGUYEN VAN HOANG', 4.9, 'Shipper nhiệt tình chuyên nghiệp thùng cách nhiệt bảo quản rau tươi', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252921/brands/xu1pv9ty2njacb3bhozh.jpg', NULL, 'A1-99999999', '59-X1 123.45', NOW())
ON DUPLICATE KEY UPDATE email=VALUES(email);

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
-- Shop 1 (id: 4)
(101, 4, 1, 'Rau muống sạch Đà Lạt', 'bó', 12000, 100, 'Rau muống tươi xanh ngọn non mướt, thu hoạch sáng sớm từ vườn hữu cơ Đà Lạt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(102, 4, 1, 'Cải ngọt tươi giòn', 'bó', 15000, 80, 'Cải ngọt trồng theo hướng hữu cơ, lá mướt xanh không dập nát, phù hợp xào nấu canh.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 10),
(103, 4, 1, 'Xà lách mỡ giòn ngọt', 'túi', 18000, 60, 'Xà lách mỡ tươi rửa sạch ăn sống ngay, thích hợp làm salad giải nhiệt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 0),
(104, 4, 1, 'Cải bó xôi (Spinach)', 'túi', 22000, 50, 'Cải bó xôi giàu sắt và dinh dưỡng cho bé và cả gia đình.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(105, 4, 2, 'Cà rốt Đà Lạt hữu cơ', 'kg', 25000, 120, 'Cà rốt vỏ cam tự nhiên, ngọt đậm vị củ tươi vừa nhổ, giàu beta-carotene.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 5),
(106, 4, 2, 'Khoai tây vàng Đà Lạt', 'kg', 30000, 90, 'Khoai tây bùi thơm, ruột vàng ruộm, vỏ mỏng không sâu mọt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), 0),
(107, 4, 2, 'Khoai lang mật tím', 'kg', 28000, 75, 'Khoai lang mật tím dẻo ngọt, nướng hoặc hấp thơm lừng.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), 0),
(108, 4, 3, 'Cà chua bi ngọt mọng nước', 'kg', 35000, 60, 'Cà chua chín cây đỏ mọng, giàu vitamin C và chất chống oxy hóa lycopene.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(109, 4, 3, 'Dâu tây giống Nhật Đà Lạt', 'kg', 180000, 30, 'Dâu tây đỏ mọng hái sáng sớm tại nhà kính Đà Lạt, ngọt thanh chua nhẹ.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 15),

-- Shop 2 (id: 5)
(201, 5, 1, 'Rau ngót vườn hữu cơ', 'bó', 10000, 80, 'Rau ngót lá nhỏ xanh đậm, nấu canh thịt bằm ngọt mát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 0),
(202, 5, 1, 'Mồng tơi ngọn non', 'bó', 10000, 90, 'Mồng tơi xanh non, thích hợp nấu canh cua rau đay mồng tơi thanh mát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 0),
(203, 5, 2, 'Bí đỏ hồ lô dẻo ngọt', 'kg', 22000, 70, 'Bí đỏ hồ lô hạt lép thịt dày, giàu vitamin A, nấu chè hầm canh đều ngon.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), 0),
(204, 5, 2, 'Củ cải trắng tươi mát', 'kg', 18000, 85, 'Củ cải trắng vị ngọt giòn thanh, dùng muối dưa, nấu súp hầm xương.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 0),
(205, 5, 3, 'Bưởi da xanh Bến Tre', 'kg', 65000, 50, 'Bưởi da xanh tép hồng mọng nước, vị ngọt thanh không chua, múi tróc vỏ.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), 0),
(206, 5, 3, 'Cam sành Tiền Giang', 'kg', 35000, 150, 'Cam sành vỏ mỏng mọng nước, vị chua ngọt tự nhiên giàu vitamin C giải khát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 0),
(207, 5, 3, 'Xoài cát Hòa Lộc Tiền Giang', 'kg', 85000, 40, 'Xoài cát Hòa Lộc nức tiếng thịt vàng ươm, thơm ngát, vị ngọt đậm đà.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 10),
(208, 5, 3, 'Dưa hấu ruột đỏ ngọt lịm', 'kg', 14000, 180, 'Dưa hấu vỏ mỏng ruột đỏ đặc ruột, ngọt lịm giải nhiệt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 0)
ON DUPLICATE KEY UPDATE product_name=VALUES(product_name), selling_price=VALUES(selling_price);

-- 3.5 PRODUCT IMAGES
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
(208, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917373/brands/h0mivuwgipa6hssaawfi.jpg', 1, 1);

-- 3.6 NUTRITION INFO
INSERT INTO nutrition_info (product_id, calories, protein_g, carbs_g, fat_g) VALUES
(101, 19.0, 2.6, 3.1, 0.2),
(102, 16.0, 1.5, 2.8, 0.2),
(105, 41.0, 0.9, 9.6, 0.2),
(106, 77.0, 2.0, 17.5, 0.1),
(108, 18.0, 0.9, 3.9, 0.2),
(109, 32.0, 0.7, 7.7, 0.3)
ON DUPLICATE KEY UPDATE calories=VALUES(calories);

-- 3.7 VOUCHERS
INSERT INTO vouchers (voucher_id, shop_id, voucher_code, discount_value, max_discount, min_order_value, usage_limit, expiry_date, voucher_type, active) VALUES
(1, 4, 'CAPNONG10K', 10000, 10000, 50000, 100, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FIXED_AMOUNT', 1),
(2, 4, 'DALATFREESHIP', 15000, 15000, 100000, 50, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FREE_SHIPPING', 1),
(3, 5, 'MIENTAY15K', 15000, 15000, 80000, 100, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FIXED_AMOUNT', 1)
ON DUPLICATE KEY UPDATE voucher_code=VALUES(voucher_code);

-- 3.8 CARTS & SAMPLE CART ITEMS (FOR BUYER 1 & 2)
INSERT INTO carts (id, user_id) VALUES
(1, 2),
(2, 3)
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

INSERT INTO cart_items (cart_id, product_id, quantity) VALUES
(1, 101, 2),  -- Rau muống (Shop 1)
(1, 105, 1),  -- Cà rốt (Shop 1)
(1, 205, 1);  -- Bưởi da xanh (Shop 2)

-- 3.9 WALLETS
INSERT INTO wallets (user_id, status, type, total_balance, frozen_balance, total_revenue_all_time, total_withdrawn, commission_wallet) VALUES
(1, 'ACTIVE', 'PLATFORM', 100000000, 0, 0, 0, 50000),
(4, 'ACTIVE', 'SHOP', 1500000, 0, 1500000, 0, 45000),
(5, 'ACTIVE', 'SHOP', 1200000, 0, 1200000, 0, 36000),
(6, 'ACTIVE', 'SHIPPER', 250000, 0, 250000, 0, 0)
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

-- 3.10 ALLERGENS & HEALTH CONDITIONS
INSERT INTO allergens (id, name) VALUES
(1, 'Đậu phộng / Lạc'), (2, 'Phấn hoa thực vật'), (3, 'Đậu nành'), (4, 'Gluten')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO health_conditions (id, name) VALUES
(1, 'Tiểu đường type 2 (Cần giảm đường bột)'),
(2, 'Huyết áp cao (Ăn nhạt, nhiều kali)'),
(3, 'Đau dạ dày / Tiêu hóa nhạy cảm'),
(4, 'Ăn kiêng giảm cân (Keto / Low-carb)')
ON DUPLICATE KEY UPDATE name=VALUES(name);

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
