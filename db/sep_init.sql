-- ==============================================================================
-- CAPNONG (SEP490) - INITIAL SEED DATA SCRIPT (sep_init.sql)
-- Dành cho việc nạp dữ liệu khởi tạo trên DB đã được Hibernate tự động tạo bảng
-- (spring.jpa.hibernate.ddl-auto: update)
-- Tập trung vào các tính năng đang hoạt động:
-- Auth, Catalog (Categories, Products, Images), Cart, Checkout, Order, Payment, Voucher
-- Mật khẩu mặc định: Admin@123
-- ==============================================================================

USE DB_CapNong;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. ROLES
INSERT INTO roles (id, name, description) VALUES
(1, 'BUYER', 'Role dành cho Người mua hàng'),
(2, 'SHOP', 'Role dành cho Cửa hàng / Nhà vườn CapNong'),
(3, 'SHIPPER', 'Role dành cho Người giao hàng'),
(4, 'ADMIN', 'Role dành cho Quản trị viên hệ thống')
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description);

-- 2. USERS
-- Admin: admin@gmail.com / Admin@123
-- Buyer: buyer1@gmail.com / Admin@123
-- Shop 1: shop1@gmail.com / Admin@123
-- Shop 2: shop2@gmail.com / Admin@123
-- Shipper: shipper1@gmail.com / Admin@123
INSERT INTO users (id, email, password, role_id, status, full_name, phone_number, address, shop_name, shop_active, bank_name, bank_account, bank_account_holder, rating_average, description, logo_url, achievement, license, vehicle_number, create_at) VALUES
(1, 'admin@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 4, 'ACTIVE', 'Quản Trị Viên Hệ Thống', '0900000000', 'Quận 1, TP. Hồ Chí Minh', NULL, 0, 'Vietcombank', '0011001234567', 'CAPNONG ADMIN', 5.0, 'Quản trị viên toàn hệ thống CapNong', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', 'SuperAdmin', NULL, NULL, NOW()),

(2, 'buyer1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 1, 'ACTIVE', 'Ngô Gia Sang (Khách mua)', '0911111111', 'Đ. Võ Trường Toản, Linh Trung, TP. Thủ Đức, TP.HCM', NULL, 0, 'VietinBank', '101888888888', 'NGO GIA SANG', 5.0, 'Khách hàng thân thiết', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', NULL, NULL, NULL, NOW()),

(3, 'buyer2@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 1, 'ACTIVE', 'Trần Thị Mai', '0911111112', '120 Hoàng Diệu 2, Linh Chiểu, Thủ Đức, TP.HCM', NULL, 0, 'MBBank', '970422888888', 'TRAN THI MAI', 5.0, 'Khách hàng', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', NULL, NULL, NULL, NOW()),

(4, 'shop1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 2, 'ACTIVE', 'Trần Văn Kiệt (Nhà Vườn 1)', '0933333333', 'Phường Linh Trung, Thủ Đức, TP.HCM', 'Nông Sản Xấu Mã - Vườn Đà Lạt', 1, 'Vietcombank', '0071000123456', 'TRAN VAN KIET', 4.9, 'Chuyên rau củ quả organic sạch từ Đà Lạt và các vùng nông nghiệp sạch chuẩn VietGAP.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252594/brands/srflhqfol1ckwavsaa1n.jpg', 'Hạt Giống Vàng', NULL, NULL, NOW()),

(5, 'shop2@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 2, 'ACTIVE', 'Lê Thị Ngọc (Nhà Vườn 2)', '0944444444', '62 Nguyễn Bỉnh Khiêm, Dĩ An, Bình Dương', 'Vườn Xanh Bình Dương & Miền Tây', 1, 'Techcombank', '19034567890123', 'LE THI NGOC', 4.8, 'Chuyên rau củ quả tươi miền Nam, trái cây đặc sản miền Tây chất lượng cao, thu hái trong ngày.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252337/brands/t9muel0yqmqfzfu3ndij.jpg', 'Nông Dân Tinh Hoa', NULL, NULL, NOW()),

(6, 'shipper1@gmail.com', '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K', 3, 'ACTIVE', 'Nguyễn Văn Hoàng (Shipper)', '0922222222', 'Linh Trung, Thủ Đức, TP.HCM', NULL, 0, 'VietinBank', '101899999999', 'NGUYEN VAN HOANG', 4.9, 'Shipper nhiệt tình chuyên nghiệp thùng cách nhiệt bảo quản rau tươi', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252921/brands/xu1pv9ty2njacb3bhozh.jpg', NULL, 'A1-99999999', '59-X1 123.45', NOW())
ON DUPLICATE KEY UPDATE email=VALUES(email), password=VALUES(password), role_id=VALUES(role_id), status=VALUES(status);

-- 3. CATEGORIES
INSERT INTO categories (category_id, name, description) VALUES
(1, 'Rau lá', 'Các loại rau xanh tươi sạch: rau muống, cải, xà lách, rau thơm thu hoạch sáng sớm.'),
(2, 'Củ', 'Các loại củ tươi ngon: cà rốt, khoai tây, khoai lang, củ cải, hành tỏi Đà Lạt.'),
(3, 'Quả / Trái cây', 'Trái cây tươi đặc sản: chuối, bưởi da xanh, xoài cát, cam sành, dâu tây mọng nước.'),
(4, 'Nông sản hữu cơ VietGAP', 'Nông sản canh tác tự nhiên đạt chuẩn chứng nhận an toàn thực phẩm.'),
(5, 'Combo & Túi mù', 'Gói rau củ theo món ăn và túi mù nông sản tiết kiệm bảo vệ môi trường.')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 4. PRODUCTS
INSERT INTO products (product_id, shop_id, category_id, product_name, unit, selling_price, stock_quantity, description, status, expiry_date, discount_percent) VALUES
(101, 4, 1, 'Rau muống sạch Đà Lạt', 'bó', 12000, 100, 'Rau muống tươi xanh ngọn non mướt, thu hoạch sáng sớm từ vườn hữu cơ Đà Lạt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(102, 4, 1, 'Cải ngọt tươi giòn', 'bó', 15000, 80, 'Cải ngọt trồng theo hướng hữu cơ, lá mướt xanh không dập nát, phù hợp xào nấu canh.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 10),
(103, 4, 1, 'Xà lách mỡ giòn ngọt', 'túi', 18000, 60, 'Xà lách mỡ tươi rửa sạch ăn sống ngay, thích hợp làm salad giải nhiệt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 0),
(104, 4, 1, 'Cải bó xôi (Spinach)', 'túi', 22000, 50, 'Cải bó xôi giàu sắt và dinh dưỡng cho bé và cả gia đình.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(105, 4, 2, 'Cà rốt Đà Lạt hữu cơ', 'kg', 25000, 120, 'Cà rốt vỏ cam tự nhiên, ngọt đậm vị củ tươi vừa nhổ, giàu beta-carotene.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 5),
(106, 4, 2, 'Khoai tây vàng Đà Lạt', 'kg', 30000, 90, 'Khoai tây bùi thơm, ruột vàng ruộm, vỏ mỏng không sâu mọt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), 0),
(107, 4, 2, 'Khoai lang mật tím', 'kg', 28000, 75, 'Khoai lang mật tím dẻo ngọt, nướng hoặc hấp thơm lừng.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), 0),
(108, 4, 3, 'Cà chua bi ngọt mọng nước', 'kg', 35000, 60, 'Cà chua chín cây đỏ mọng, giàu vitamin C và chất chống oxy hóa lycopene.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(109, 4, 3, 'Dâu tây giống Nhật Đà Lạt', 'kg', 180000, 30, 'Dâu tây đỏ mọng hái sáng sớm tại nhà kính Đà Lạt, ngọt thanh chua nhẹ.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 15),

(201, 5, 1, 'Rau ngót vườn hữu cơ', 'bó', 10000, 80, 'Rau ngót lá nhỏ xanh đậm, nấu canh thịt bằm ngọt mát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 0),
(202, 5, 1, 'Mồng tơi ngọn non', 'bó', 10000, 90, 'Mồng tơi xanh non, thích hợp nấu canh cua rau đay mồng tơi thanh mát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 0),
(203, 5, 2, 'Bí đỏ hồ lô dẻo ngọt', 'kg', 22000, 70, 'Bí đỏ hồ lô hạt lép thịt dày, giàu vitamin A, nấu chè hầm canh đều ngon.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), 0),
(204, 5, 2, 'Củ cải trắng tươi mát', 'kg', 18000, 85, 'Củ cải trắng vị ngọt giòn thanh, dùng muối dưa, nấu súp hầm xương.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 0),
(205, 5, 3, 'Bưởi da xanh Bến Tre', 'kg', 65000, 50, 'Bưởi da xanh tép hồng mọng nước, vị ngọt thanh không chua, múi tróc vỏ.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), 0),
(206, 5, 3, 'Cam sành Tiền Giang', 'kg', 35000, 150, 'Cam sành vỏ mỏng mọng nước, vị chua ngọt tự nhiên giàu vitamin C giải khát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 0),
(207, 5, 3, 'Xoài cát Hòa Lộc Tiền Giang', 'kg', 85000, 40, 'Xoài cát Hòa Lộc nức tiếng thịt vàng ươm, thơm ngát, vị ngọt đậm đà.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 10),
(208, 5, 3, 'Dưa hấu ruột đỏ ngọt lịm', 'kg', 14000, 180, 'Dưa hấu vỏ mỏng ruột đỏ đặc ruột, ngọt lịm giải nhiệt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 0)
ON DUPLICATE KEY UPDATE product_name=VALUES(product_name), selling_price=VALUES(selling_price);

-- 5. PRODUCT IMAGES
INSERT INTO product_images (product_id, image_url, is_primary, display_order, is_approved, is_flagged) VALUES
(101, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943166/brands/ezrip6hvips9nwmyottf.jpg', 1, 1, 1, 0),
(102, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943139/brands/mhfnqgigq3xwxivi3r0u.jpg', 1, 1, 1, 0),
(103, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916585/brands/ojx1comtiw3wbpq4rlzy.jpg', 1, 1, 1, 0),
(104, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249130/brands/vleciprlqifxmkwsmlxz.webp', 1, 1, 1, 0),
(105, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916721/brands/uj1eoinkwvthfjvtpvpq.jpg', 1, 1, 1, 0),
(106, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916676/brands/bmgnazybi0rro8bnxhwp.jpg', 1, 1, 1, 0),
(107, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249499/brands/pgdvlccom3lid5t24pnk.jpg', 1, 1, 1, 0),
(108, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916530/brands/xjiiaehe5evwybwhmpmu.jpg', 1, 1, 1, 0),
(109, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943061/brands/qtspypfmdjcxtsbme7xi.jpg', 1, 1, 1, 0),

(201, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250103/brands/qlsbgtgno3kvconpo0c3.jpg', 1, 1, 1, 0),
(202, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250212/brands/mtbfbygr3tmlctmfdg0x.jpg', 1, 1, 1, 0),
(203, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917463/brands/xjz74sedvolg7woxywpo.jpg', 1, 1, 1, 0),
(204, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943219/brands/dysl261o6o2bapfwurmf.jpg', 1, 1, 1, 0),
(205, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917344/brands/c2i9gkbkl8vx7iwulwsz.png', 1, 1, 1, 0),
(206, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916481/brands/s5ld5232nl4s4fwczawr.jpg', 1, 1, 1, 0),
(207, 'https://images.unsplash.com/photo-1553279768-865429fa0078?w=600&h=400&fit=crop', 1, 1, 1, 0),
(208, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917373/brands/h0mivuwgipa6hssaawfi.jpg', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE product_id=VALUES(product_id);


-- 6. CARTS & SAMPLE CART ITEMS (FOR BUYER 1 & 2)
INSERT INTO carts (id, user_id) VALUES
(1, 2),
(2, 3)
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

INSERT INTO cart_items (cart_id, product_id, quantity) VALUES
(1, 101, 2),  -- Rau muống (Shop 1)
(1, 105, 1),  -- Cà rốt (Shop 1)
(1, 205, 1);  -- Bưởi da xanh (Shop 2)

-- 7. VOUCHERS
INSERT INTO vouchers (voucher_id, shop_id, voucher_code, discount_value, max_discount, min_order_value, usage_limit, expiry_date, voucher_type, active) VALUES
(1, 4, 'CAPNONG10K', 10000, 10000, 50000, 100, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FIXED_AMOUNT', 1),
(2, 4, 'DALATFREESHIP', 15000, 15000, 100000, 50, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FREE_SHIPPING', 1),
(3, 5, 'MIENTAY15K', 15000, 15000, 80000, 100, DATE_ADD(NOW(), INTERVAL 30 DAY), 'FIXED_AMOUNT', 1)
ON DUPLICATE KEY UPDATE voucher_code=VALUES(voucher_code);

-- 8. ORDERS (ĐƠN HÀNG PHỤC VỤ TEST TÍNH NĂNG ĐÁNH GIÁ & FEEDBACK)
INSERT INTO orders (order_id, buyer_id, shop_id, shipper_id, shipping_fee, recipient_name, recipient_phone, shipping_address, note, status, total_amount, payment_method, shipping_discount_amount, batch_discount_amount, created_at) VALUES
-- Đơn 106: DELIVERED (Shop 2: Vườn Miền Tây) -> Đã đánh giá (Bưởi da xanh)
(106, 2, 5, 6, 15000, 'Ngô Gia Sang (Khách mua)', '0911111111', 'Đ. Võ Trường Toản, Linh Trung, TP. Thủ Đức, TP.HCM', 'Giao giờ hành chính', 'DELIVERED', 135000, 'COD', 0, 15000, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- Đơn 108: DELIVERED (Shop 1: Vườn Đà Lạt) -> 1 món ĐÃ đánh giá & 1 món CHƯA đánh giá để test nút Đánh giá
(108, 2, 4, 6, 15000, 'Ngô Gia Sang (Khách mua)', '0911111111', 'Đ. Võ Trường Toản, Linh Trung, TP. Thủ Đức, TP.HCM', 'Giao cẩn thận', 'DELIVERED', 62000, 'COD', 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- Đơn 109: COMPLETED (Shop 1: Vườn Đà Lạt) -> Đã đánh giá, CHƯA có phản hồi từ shop (để Farmer test form reply)
(109, 2, 4, 6, 15000, 'Ngô Gia Sang (Khách mua)', '0911111111', 'Đ. Võ Trường Toản, Linh Trung, TP. Thủ Đức, TP.HCM', 'Để ở bàn lễ tân', 'COMPLETED', 50000, 'PAYOS', 0, 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- Đơn 110: DELIVERED (Shop 2: Vườn Miền Tây) của Buyer 2 (Trần Thị Mai) -> Đã đánh giá 5 sao (Cam sành)
(110, 3, 5, 6, 15000, 'Trần Thị Mai', '0911111112', '120 Hoàng Diệu 2, Linh Chiểu, Thủ Đức, TP.HCM', 'Giao giờ hành chính', 'DELIVERED', 120000, 'PAYOS', 0, 0, DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- Đơn 111: DELIVERED (Shop 1: Vườn Đà Lạt) của Buyer 2 (Trần Thị Mai) -> Đã đánh giá 3 sao (Khoai tây vàng)
(111, 3, 4, 6, 15000, 'Trần Thị Mai', '0911111112', '120 Hoàng Diệu 2, Linh Chiểu, Thủ Đức, TP.HCM', 'Giao nhẹ tay', 'DELIVERED', 75000, 'COD', 0, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- Đơn 112: DELIVERED (Shop 2: Vườn Miền Tây) của Buyer 1 -> CHƯA đánh giá (Xoài cát Hòa Lộc để test đánh giá Shop 2)
(112, 2, 5, 6, 15000, 'Ngô Gia Sang (Khách mua)', '0911111111', 'Đ. Võ Trường Toản, Linh Trung, TP. Thủ Đức, TP.HCM', 'Giao buổi chiều', 'DELIVERED', 185000, 'PAYOS', 0, 0, DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- 9. ORDER DETAILS
INSERT INTO order_details (order_detail_id, order_id, product_id, mystery_id, combo_id, quantity, unit_price, item_type) VALUES
(9, 106, 205, NULL, NULL, 2, 65000, 'PRODUCT'),        -- Bưởi da xanh (Đơn 106, ĐÃ đánh giá 5 sao)
(10, 108, 105, NULL, NULL, 2, 25000, 'PRODUCT'),       -- Cà rốt Đà Lạt (Đơn 108, ĐÃ đánh giá 5 sao)
(11, 108, 101, NULL, NULL, 1, 12000, 'PRODUCT'),       -- Rau muống sạch Đà Lạt (Đơn 108, CHƯA ĐÁNH GIÁ -> Test nút Đánh giá)
(12, 109, 108, NULL, NULL, 1, 35000, 'PRODUCT'),       -- Cà chua bi ngọt (Đơn 109, ĐÃ đánh giá 4 sao, CHƯA có phản hồi từ shop)
(13, 110, 206, NULL, NULL, 3, 35000, 'PRODUCT'),       -- Cam sành Tiền Giang (Đơn 110, ĐÃ đánh giá 5 sao)
(14, 111, 106, NULL, NULL, 2, 30000, 'PRODUCT'),       -- Khoai tây vàng Đà Lạt (Đơn 111, ĐÃ đánh giá 3 sao)
(15, 112, 207, NULL, NULL, 2, 85000, 'PRODUCT')        -- Xoài cát Hòa Lộc (Đơn 112, CHƯA ĐÁNH GIÁ -> Test nút Đánh giá)
ON DUPLICATE KEY UPDATE quantity=VALUES(quantity);

-- 10. REVIEWS & FEEDBACK (ĐÁNH GIÁ ĐƠN HÀNG VÀ PHẢN HỒI NÔNG DÂN)
INSERT INTO reviews (review_id, buyer_id, shop_id, order_detail_id, product_id, mystery_id, combo_id, rating_star, comment, evidence, create_at, reply_from_shop) VALUES
(1, 2, 4, 10, 105, NULL, NULL, 5.0, 'Cà rốt vỏ cam tự nhiên, ngọt đậm vị củ tươi vừa nhổ, ép nước siêu ngon! Đóng gói thùng giấy rất cẩn thận bảo vệ môi trường.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916721/brands/uj1eoinkwvthfjvtpvpq.jpg', DATE_SUB(NOW(), INTERVAL 2 DAY), 'Cảm ơn bạn Sang đã luôn ủng hộ nông sản sạch Đà Lạt của nhà vườn ạ! Vườn sẽ tiếp tục chọn những củ tươi ngon nhất gửi đến bạn nhé.'),

(2, 2, 4, 12, 108, NULL, NULL, 4.0, 'Cà chua bi mọng nước, vị chua ngọt thanh mát ăn sống làm salad rất thích. Vận chuyển hơi lâu một chút xíu nhưng bù lại rau củ vẫn giữ được độ tươi ngon.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916530/brands/xjiiaehe5evwybwhmpmu.jpg', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),

(3, 3, 4, 14, 106, NULL, NULL, 3.0, 'Khoai tây bở bùi thơm ngon, tuy nhiên có 1 củ bị cọ xát hơi trầy vỏ ngoài trong quá trình vận chuyển. Shop lần sau bọc thêm xốp lưới giúp mình nhé.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916676/brands/bmgnazybi0rro8bnxhwp.jpg', DATE_SUB(NOW(), INTERVAL 4 DAY), 'Dạ nhà vườn thành thật xin lỗi chị Mai ạ! Bên em đã ghi nhận phản hồi và từ đơn sau sẽ bọc thêm xốp lưới chống dập cho củ ạ.'),

(4, 3, 5, 13, 206, NULL, NULL, 5.0, 'Cam sành vỏ mỏng, tép mọng nước, vắt được rất nhiều nước ngọt thanh không bị đắng. 10 điểm chất lượng cho nhà vườn Miền Tây!', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916481/brands/s5ld5232nl4s4fwczawr.jpg', DATE_SUB(NOW(), INTERVAL 2 DAY), 'Dạ cảm ơn chị Mai nhiều ạ! Cam vườn nhà em cắt cuống lá hái trong ngày nên luôn tươi mọng nước ạ.'),

(5, 2, 5, 9, 205, NULL, NULL, 5.0, 'Bưởi da xanh tép hồng đều, tróc múi dễ bóc, vị ngọt lịm không chua chút nào. Rất hài lòng và sẽ tiếp tục mua ủng hộ bà con nông dân!', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917344/brands/c2i9gkbkl8vx7iwulwsz.png', DATE_SUB(NOW(), INTERVAL 1 DAY), 'Vườn Xanh cảm ơn anh Sang đã tin dùng sản phẩm bưởi da xanh Bến Tre của tụi em ạ!')
ON DUPLICATE KEY UPDATE rating_star=VALUES(rating_star), comment=VALUES(comment), reply_from_shop=VALUES(reply_from_shop);

-- 11. REVIEW REACTIONS (LIKE / DISLIKE)
INSERT INTO review_reactions (id, review_id, buyer_id, reaction_type, created_at) VALUES
(1, 1, 3, 'LIKE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 1, 2, 'LIKE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 2, 3, 'LIKE', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 4, 2, 'LIKE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 5, 3, 'LIKE', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE reaction_type=VALUES(reaction_type);

-- 12. REVIEW IMAGES (ĐÍNH KÈM ĐA ẢNH REVIEW)
INSERT INTO images (id, image_url, media_type, created_at, review_id) VALUES
(1001, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916721/brands/uj1eoinkwvthfjvtpvpq.jpg', 'PHOTO', DATE_SUB(NOW(), INTERVAL 2 DAY), 1),
(1002, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916530/brands/xjiiaehe5evwybwhmpmu.jpg', 'PHOTO', DATE_SUB(NOW(), INTERVAL 3 DAY), 2),
(1003, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916676/brands/bmgnazybi0rro8bnxhwp.jpg', 'PHOTO', DATE_SUB(NOW(), INTERVAL 4 DAY), 3),
(1004, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916481/brands/s5ld5232nl4s4fwczawr.jpg', 'PHOTO', DATE_SUB(NOW(), INTERVAL 2 DAY), 4),
(1005, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917344/brands/c2i9gkbkl8vx7iwulwsz.png', 'PHOTO', DATE_SUB(NOW(), INTERVAL 1 DAY), 5)
ON DUPLICATE KEY UPDATE image_url=VALUES(image_url);

SET FOREIGN_KEY_CHECKS = 1;
