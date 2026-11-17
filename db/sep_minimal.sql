-- ==============================================================================
-- CAPNONG (SEP490) - SCHEMA + DỮ LIỆU TỐI THIỂU (sep_minimal.sql)
-- Cùng schema với sep.sql, chỉ seed 4 vai trò và 4 tài khoản: admin, buyer, shop, shipper
-- Không có sản phẩm, đơn hàng, ví, voucher... (ví sẽ được BE tự tạo khi cần)
-- Mật khẩu mặc định tất cả tài khoản test: Admin@123
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

-- DANGER: This seed script deletes existing data.
-- Use patch_users_orders_schema.sql to update an existing database.
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
DROP TABLE IF EXISTS dispatch_offers;
DROP TABLE IF EXISTS shipper_locations;
DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS review_reactions;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS return_requests;
DROP TABLE IF EXISTS voucher_usages;
DROP TABLE IF EXISTS voucher_users;
DROP TABLE IF EXISTS vouchers;
DROP TABLE IF EXISTS wallet_topups;
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
    google_id VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name TEXT,
    phone_number VARCHAR(50),
    address VARCHAR(255),
    cccd VARCHAR(20),
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
    cod_payment_option VARCHAR(32),
    cod_payment_status VARCHAR(32),
    cod_prepaid_amount DECIMAL(18,2) DEFAULT 0.00,
    cod_refunded_at DATETIME,
    shipper_paid_at DATETIME,
    picked_up_at DATETIME,
    delivered_at DATETIME,
    completed_at DATETIME(6),
    settled_at DATETIME(6),
    cod_reserved_amount DECIMAL(18,2) DEFAULT NULL,
    ready_at DATETIME(6),
    dispatch_wave INT,
    last_dispatched_at DATETIME(6),
    dispatch_alerted_at DATETIME(6),
    delivery_proof_urls TEXT,
    fail_reason VARCHAR(64),
    fail_note VARCHAR(500),
    fail_call_attempts INT,
    fail_proof_urls TEXT,
    failed_at DATETIME,
    rescued_weight DECIMAL(18,2) DEFAULT 0.00,
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
    status VARCHAR(16) DEFAULT 'ACTIVE',
    type VARCHAR(50),
    total_balance DECIMAL(18,2) DEFAULT 0.00,
    frozen_balance DECIMAL(18,2) DEFAULT 0.00,
    total_revenue_all_time DECIMAL(18,2) DEFAULT 0.00,
    total_withdrawn DECIMAL(18,2) DEFAULT 0.00,
    commission_wallet DECIMAL(18,2) DEFAULT 0.00,
    cod_reserved DECIMAL(18,2) DEFAULT 0.00,
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.19.1 WALLET TOPUPS
CREATE TABLE wallet_topups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    wallet_id INT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    payos_order_code VARCHAR(32) UNIQUE,
    checkout_url VARCHAR(500),
    status VARCHAR(16) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME,
    CONSTRAINT fk_wallet_topups_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id) ON DELETE CASCADE
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
    shipper_id INT,
    reason VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',
    refund_amount DECIMAL(18,2),
    is_return_goods TINYINT(1) DEFAULT 1,
    return_shipping_fee DECIMAL(18,2) DEFAULT 0.00,
    picked_up_at DATETIME,
    returned_at DATETIME,
    shop_response VARCHAR(1000),
    admin_remark VARCHAR(1000),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_returns_detail FOREIGN KEY (order_detail_id) REFERENCES order_details(order_detail_id) ON DELETE CASCADE,
    CONSTRAINT fk_returns_shop FOREIGN KEY (shop_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_returns_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_returns_shipper FOREIGN KEY (shipper_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.23 REVIEWS & REACTIONS
CREATE TABLE reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    shop_id INT,
    order_detail_id INT,
    product_id INT,
    mystery_id INT,
    combo_id INT,
    rating_star DECIMAL(5,2),
    comment TEXT,
    evidence VARCHAR(500),
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    reply_from_shop TEXT,
    CONSTRAINT fk_reviews_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_shop FOREIGN KEY (shop_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_reviews_detail FOREIGN KEY (order_detail_id) REFERENCES order_details(order_detail_id) ON DELETE SET NULL,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE SET NULL,
    CONSTRAINT fk_reviews_mystery FOREIGN KEY (mystery_id) REFERENCES mystery_boxes(mystery_id) ON DELETE SET NULL,
    CONSTRAINT fk_reviews_combo FOREIGN KEY (combo_id) REFERENCES build_combos(combo_id) ON DELETE SET NULL
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
    media_type ENUM('IMAGE', 'PHOTO', 'VIDEO') DEFAULT 'PHOTO',
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
    delivery_phase VARCHAR(32),
    online TINYINT(1) DEFAULT 0,
    last_seen_at DATETIME,
    CONSTRAINT fk_ship_loc_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE SET NULL,
    CONSTRAINT fk_ship_loc_shipper FOREIGN KEY (shipper_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.25.1 DISPATCH OFFERS
CREATE TABLE dispatch_offers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    shipper_id INT NOT NULL,
    wave INT DEFAULT 1,
    distance_km DOUBLE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_dispatch_offer_order_shipper UNIQUE (order_id, shipper_id),
    CONSTRAINT fk_disp_off_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_disp_off_shipper FOREIGN KEY (shipper_id) REFERENCES users(id) ON DELETE CASCADE
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
-- 3. SEED DATA TỐI THIỂU
-- ==============================================================================

-- 3.1 ROLES
INSERT INTO roles (id, name, description) VALUES
(1, 'BUYER', 'Role dành cho Người mua hàng'),
(2, 'SHOP', 'Role dành cho Cửa hàng / Nhà vườn CapNong'),
(3, 'SHIPPER', 'Role dành cho Người giao hàng'),
(4, 'ADMIN', 'Role dành cho Quản trị viên hệ thống');

-- 3.2 USERS (mật khẩu: Admin@123)
INSERT INTO users (id, email, password, role_id, status, full_name, phone_number, address, shop_name, shop_active, bank_name, bank_account, bank_account_holder, rating_average, description, logo_url, achievement, license, vehicle_number, diet_preference, create_at) VALUES
(1, 'admin@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 4, 'ACTIVE', 'Quản Trị Viên Hệ Thống', '0900000000', '1 Công Xã Paris, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh', NULL, 0, 'Vietcombank', '0011001234567', 'CAPNONG ADMIN', 5.0, 'Quản trị viên toàn hệ thống CapNong', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', 'SuperAdmin', NULL, NULL, NULL, NOW()),

(2, 'buyer1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 1, 'ACTIVE', 'Ngô Gia Sang (Khách mua)', '0911111111', '268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh', NULL, 0, 'VietinBank', '101888888888', 'NGO GIA SANG', 5.0, 'Khách hàng thân thiết', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', NULL, NULL, NULL, 'VEGETARIAN', NOW()),

(3, 'shop1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 2, 'ACTIVE', 'Trần Văn Kiệt (Nhà Vườn 1)', '0933333333', 'Số 1 Võ Văn Ngân, Phường Linh Chiểu, Thành phố Thủ Đức, TP. Hồ Chí Minh', 'Nông Sản Xấu Mã - Vườn Đà Lạt', 1, 'Vietcombank', '0071000123456', 'TRAN VAN KIET', 4.9, 'Chuyên rau củ quả organic sạch từ Đà Lạt và các vùng nông nghiệp sạch chuẩn VietGAP.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252594/brands/srflhqfol1ckwavsaa1n.jpg', 'Hạt Giống Vàng', NULL, NULL, NULL, NOW()),

(4, 'shipper1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 3, 'ACTIVE', 'Nguyễn Văn Hoàng (Shipper)', '0922222222', 'Số 1 Võ Văn Ngân, Phường Linh Chiểu, Thành phố Thủ Đức, TP. Hồ Chí Minh', NULL, 0, 'VietinBank', '101899999999', 'NGUYEN VAN HOANG', 4.9, 'Shipper nhiệt tình chuyên nghiệp thùng cách nhiệt bảo quản rau tươi', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252921/brands/xu1pv9ty2njacb3bhozh.jpg', NULL, 'A1-99999999', '59-X1 123.45', NULL, NOW());

SET FOREIGN_KEY_CHECKS = 1;
