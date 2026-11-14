-- ==============================================================================
-- CAPNONG (SEP490) - INITIAL SEED DATA SCRIPT (sep_init.sql)
-- Dành cho việc nạp dữ liệu khởi tạo trên DB đã được Hibernate tự động tạo bảng
-- (spring.jpa.hibernate.ddl-auto: update)
-- Tập trung vào các tính năng đang hoạt động:
-- Auth, Catalog (Categories, Products, Images), Cart, Checkout, Order, Payment, Voucher, Notifications, Blogs
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
(110, 4, 1, 'Bông cải xanh Đà Lạt (Broccoli)', 'kg', 45000, 80, 'Bông cải xanh Đà Lạt cuống giòn ngọt, búp hoa dày chắc, giàu vitamin C và chất xơ chống lão hóa.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 0),
(111, 4, 1, 'Bắp cải trái tim Đà Lạt', 'bắp', 22000, 110, 'Bắp cải trái tim cuốn chặt lá mỏng giòn ngọt tự nhiên, thích hợp luộc chấm trứng hoặc xào.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 0),
(112, 4, 3, 'Ớt chuông Đà Lạt đủ màu', 'kg', 55000, 65, 'Ớt chuông đỏ vàng xanh ngọt mọng nước, không hăng cay, giàu vitamin C tuyệt vời cho món xào và salad.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 10),
(113, 4, 2, 'Hành tây vàng Đà Lạt', 'kg', 28000, 95, 'Hành tây vàng củ tròn chắc, vị ngọt thanh không hăng gắt, vỏ khô ráo bảo quản được lâu.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 25 DAY), 0),
(114, 4, 2, 'Tỏi cô đơn Phan Rang', 'kg', 110000, 50, 'Tỏi cô đơn một nhánh tép to mẩy, hương thơm cay nồng đặc trưng, vị kháng sinh tự nhiên tốt cho sức khỏe.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 45 DAY), 0),
(115, 4, 3, 'Bơ sáp 034 Đắk Lắk dẻo béo', 'kg', 55000, 85, 'Bơ 034 trái dài hạt lép, cơm vàng ươm dẻo quánh béo ngậy, thu hái đúng độ già.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(116, 4, 3, 'Dưa leo baby giòn ngọt', 'kg', 22000, 90, 'Dưa leo baby vỏ mỏng ruột đặc, giòn ngọt thanh mát không đắng, ăn trực tiếp hoặc cuốn gỏi.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(117, 4, 4, 'Nấm đùi gà hữu cơ tươi', 'khay', 32000, 60, 'Nấm đùi gà thân trắng mập mạp giòn sần sật, vị ngọt thịt thanh khiết, nấu lẩu xào chay đều ngon.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(118, 4, 4, 'Bắp ngọt Mỹ giòn hạt', 'bắp', 12000, 120, 'Bắp ngọt Mỹ bắp to đều hạt mẩy căng mọng, vị ngọt thanh mát luộc ăn vặt bổ dưỡng.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(119, 4, 3, 'Cà tím quả dài Đà Lạt', 'kg', 24000, 70, 'Cà tím quả thẳng vỏ bóng tím mướt, ruột mềm ngọt nướng mỡ hành hoặc kho thịt ba chỉ.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 0),
(120, 4, 1, 'Măng tây xanh loại 1', 'bó', 48000, 40, 'Măng tây xanh ngọn non giòn rụm, giàu axit folic và khoáng chất, xào tỏi hoặc bò tuyệt hảo.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 0),
(121, 4, 3, 'Khổ qua rừng Đà Lạt tươi giòn', 'kg', 35000, 70, 'Khổ qua rừng trái nhỏ gai nhọn, vị đắng thanh đậm đà, cực kỳ tốt cho người tiểu đường và thanh nhiệt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 0),
(122, 4, 3, 'Dưa lưới hoàng kim ruột cam', 'kg', 65000, 85, 'Dưa lưới giống Nhật vỏ vân lưới dày đẹp, thịt quả màu cam giòn tan ngọt lịm mọng nước.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 5),
(123, 4, 3, 'Chanh dây tím Đà Lạt mọng nước', 'kg', 38000, 90, 'Chanh dây tím quả tròn căng mẩy, dịch quả vàng ươm thơm ngát, vị chua ngọt hài hòa làm nước giải khát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), 0),
(124, 4, 4, 'Nấm rơm tươi loại 1 hữu cơ', 'kg', 45000, 60, 'Nấm rơm búp tròn chắc thịt, thu hái sáng sớm không ngâm hóa chất, nấu canh xào kho đều ngọt thanh.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 0),
(125, 4, 1, 'Bí ngòi xanh Đà Lạt (Zucchini)', 'kg', 24000, 95, 'Bí ngòi xanh quả thon dài vỏ mỏng bóng mướt, ruột mềm ngọt thích hợp làm món xào nướng hoặc nấu súp.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 0),
(126, 4, 2, 'Củ dền đỏ Đà Lạt bổ dưỡng', 'kg', 28000, 80, 'Củ dền đỏ thẫm tươi cứng cáp, giàu chất sắt và khoáng chất, ép nước hoặc hầm súp canh màu sắc bắt mắt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), 0),
(127, 4, 3, 'Mận hậu Mộc Châu giòn ngọt', 'kg', 55000, 60, 'Mận hậu quả to tròn căng mọng phủ phấn trắng, thịt quả đỏ au giòn rụm vị chua ngọt đậm đà.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(128, 4, 3, 'Đào hồng Sa Pa mọng ngọt', 'kg', 65000, 50, 'Đào hồng Sa Pa lông tơ mịn màng má ửng hồng, thịt quả giòn ngọt thơm lừng hương vị núi rừng Tây Bắc.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 10),
(129, 4, 1, 'Cải thìa (Bok choy) thủy canh', 'túi', 18000, 100, 'Cải thìa bẹ trắng lá xanh non mướt, cuống giòn ngọt tự nhiên không dư lượng thuốc, xào dầu hào siêu ngon.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 0),
(130, 4, 2, 'Cà rốt baby giòn ngọt ăn sống', 'túi', 32000, 75, 'Cà rốt baby củ nhỏ xinh vỏ mỏng ruột đặc, giòn ngọt thanh khiết rửa sạch ăn sống ngay hoặc làm salad.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 0),
(131, 4, 1, 'Rau húng quế sạch VietGAP', 'bó', 8000, 100, 'Rau húng quế ngọn non thơm ngát, lá xanh dày không dập nát, ăn kèm phở bún bò hoặc làm gia vị dậy mùi.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 0),
(132, 4, 2, 'Củ sả tươi Tây Ninh thơm nồng', 'bó', 12000, 80, 'Củ sả cây mập mạp bẹ trắng tinh thơm ngát tinh dầu, đập dập kho thịt cá hoặc nấu lẩu thơm lừng.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), 0),
(133, 4, 1, 'Rau bạc hà (Húng lủi) hữu cơ', 'bó', 10000, 90, 'Rau bạc hà húng lủi lá nhỏ the mát sảng khoái, ăn sống giải nhiệt hoặc pha trà decor đồ uống.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 0),

(201, 5, 1, 'Rau ngót vườn hữu cơ', 'bó', 10000, 80, 'Rau ngót lá nhỏ xanh đậm, nấu canh thịt bằm ngọt mát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 0),
(202, 5, 1, 'Mồng tơi ngọn non', 'bó', 10000, 90, 'Mồng tơi xanh non, thích hợp nấu canh cua rau đay mồng tơi thanh mát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 0),
(203, 5, 2, 'Bí đỏ hồ lô dẻo ngọt', 'kg', 22000, 70, 'Bí đỏ hồ lô hạt lép thịt dày, giàu vitamin A, nấu chè hầm canh đều ngon.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), 0),
(204, 5, 2, 'Củ cải trắng tươi mát', 'kg', 18000, 85, 'Củ cải trắng vị ngọt giòn thanh, dùng muối dưa, nấu súp hầm xương.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 0),
(205, 5, 3, 'Bưởi da xanh Bến Tre', 'kg', 65000, 50, 'Bưởi da xanh tép hồng mọng nước, vị ngọt thanh không chua, múi tróc vỏ.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), 0),
(206, 5, 3, 'Cam sành Tiền Giang', 'kg', 35000, 150, 'Cam sành vỏ mỏng mọng nước, vị chua ngọt tự nhiên giàu vitamin C giải khát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 0),
(207, 5, 3, 'Xoài cát Hòa Lộc Tiền Giang', 'kg', 85000, 40, 'Xoài cát Hòa Lộc nức tiếng thịt vàng ươm, thơm ngát, vị ngọt đậm đà.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 10),
(208, 5, 3, 'Dưa hấu ruột đỏ ngọt lịm', 'kg', 14000, 180, 'Dưa hấu vỏ mỏng ruột đỏ đặc ruột, ngọt lịm giải nhiệt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 0),
(209, 5, 3, 'Chuối già Nam Mỹ chín tự nhiên', 'nải', 32000, 100, 'Chuối già Nam Mỹ vỏ vàng mướt không tẩm thuốc, thịt ngọt dẻo thơm giàu kali.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(210, 5, 3, 'Thanh long ruột đỏ Bình Thuận', 'kg', 35000, 130, 'Thanh long ruột đỏ tai xanh quả bóng, vị ngọt đậm mọng nước giàu vitamin C.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 0),
(211, 5, 3, 'Dứa mật MD2 ngọt lịm', 'trái', 28000, 90, 'Dứa mật giống MD2 vỏ mỏng mắt nông, thơm lừng mọng nước không rát lưỡi.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 0),
(212, 5, 3, 'Sầu riêng Ri6 cơm vàng hạt lép', 'kg', 135000, 35, 'Sầu riêng Ri6 Bến Tre múi vàng rực, cơm khô ráo dẻo quánh, hương thơm nồng nàn quyến rũ.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 5),
(213, 5, 3, 'Đu đủ ruột đỏ ngọt mát', 'kg', 22000, 80, 'Đu đủ ruột đỏ hạt tiêu thịt dày chắc nịch, vị ngọt thơm thanh mát bồi bổ tiêu hóa.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 0),
(214, 5, 3, 'Ổi nữ hoàng giòn ngọt', 'kg', 25000, 110, 'Ổi nữ hoàng quả to vỏ láng mịn, ruột ít hạt giòn rụm ngọt thanh, chấm muối tôm cực ngon.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 0),
(215, 5, 3, 'Chanh không hạt mọng nước', 'kg', 22000, 120, 'Chanh không hạt vỏ mỏng thơm ngát, tép mọng nước chua thanh pha nước giải khát hoặc làm gia vị.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), 0),
(216, 5, 3, 'Nho đen không hạt Ninh Thuận', 'kg', 75000, 45, 'Nho đen chùm chắc quả căng bóng, vị ngọt dịu giòn thơm, trồng theo chuẩn an toàn.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(217, 5, 3, 'Táo đỏ giống Envy giòn ngọt', 'kg', 68000, 70, 'Táo đỏ vỏ căng mọng nước, thịt giòn tan vị ngọt đậm đà, thơm mát bồi bổ sức khỏe.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), 0),
(218, 5, 4, 'Đậu que Nhật giòn ngọt', 'kg', 30000, 75, 'Đậu que thân mập xanh non không xơ, luộc chấm kho quẹt hoặc xào tỏi giòn ngọt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 0),
(219, 5, 3, 'Ớt cay chỉ thiên tươi', 'túi', 15000, 100, 'Ớt chỉ thiên đỏ tươi quả nhỏ cay nồng, đượm vị dùng pha nước chấm thơm lừng.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), 0),
(220, 5, 3, 'Chôm chôm nhãn Tiền Giang tróc múi', 'kg', 40000, 120, 'Chôm chôm nhãn vỏ vàng đỏ râu ngắn, cơm dày khô ráo tróc múi hoàn toàn, vị ngọt đậm thơm mát.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(221, 5, 3, 'Măng cụt Lái Thiêu múi trắng tinh', 'kg', 95000, 50, 'Măng cụt nữ hoàng trái cây miền nhiệt đới, vỏ mềm mỏng múi to mọng ngọt thanh mát không sượng.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 0),
(222, 5, 3, 'Mít Thái siêu sớm múi vàng ươm', 'kg', 35000, 140, 'Mít Thái múi dày cùi mỏng hạt nhỏ, xơ vàng ngọt đậm giòn sần sật, thơm ngát mùi mít quê.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(223, 5, 3, 'Nhãn xuồng cơm vàng Bến Tre', 'kg', 50000, 80, 'Nhãn xuồng quả to tròn vỏ vàng sậm, cơm dày vàng ươm giòn ngọt nước thơm lừng đậm đà.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(224, 5, 3, 'Vải thiều chín đỏ ngọt lịm', 'kg', 65000, 70, 'Vải thiều cùi dày mọng nước hạt bé xíu, vị ngọt thanh khiết thơm mát xua tan cơn khát mùa hè.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 0),
(225, 5, 3, 'Mãng cầu xiêm chua ngọt dầm đá', 'kg', 48000, 65, 'Mãng cầu xiêm quả to gai mềm ruột trắng muốt, vị chua ngọt hài hòa tuyệt hảo làm sinh tố giải nhiệt.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 0),
(226, 5, 3, 'Hồng xiêm Tiền Giang ngọt đậm cát mịn', 'kg', 38000, 90, 'Hồng xiêm quả thon dài vỏ mỏng hạt tiêu, thịt mềm ngọt lịm như mật ong, thơm mát dễ tiêu hóa.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(227, 5, 3, 'Dừa xiêm xanh Bến Tre ngọt mát', 'trái', 16000, 200, 'Dừa xiêm xanh gọt sẵn nước trong veo ngọt lịm tự nhiên, giải khát bù khoáng cực tốt cho sức khỏe.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 25 DAY), 0),
(228, 5, 2, 'Đậu phộng tươi củ chắc hạt', 'kg', 28000, 110, 'Đậu phộng củ vừa nhổ cuống tươi rói, vỏ mỏng hạt đều béo bùi, luộc ăn vặt thơm ngọt tự nhiên.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 0),
(229, 5, 4, 'Bắp nếp dẻo thơm Tây Ninh', 'bắp', 10000, 150, 'Bắp nếp bắp thon hạt trắng đều mẩy, luộc lên dẻo thơm mềm ngọt đậm đà hương đồng gió nội.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 0),
(230, 5, 2, 'Gừng sẻ củ già thơm cay nồng', 'kg', 42000, 60, 'Gừng sẻ củ nhỏ nhánh nhiều gân, hương thơm cay nồng ấm bụng, thích hợp làm gia vị và hãm trà.', 'ACTIVE', DATE_ADD(CURRENT_DATE, INTERVAL 45 DAY), 0)
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
(110, 'https://images.unsplash.com/photo-1459411621453-7b03977f4bfc?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(111, 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(112, 'https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(113, 'https://images.unsplash.com/photo-1618512496248-a07fe83aa8cb?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(114, 'https://images.unsplash.com/photo-1615485500704-8e990f9900f7?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(115, 'https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(116, 'https://images.unsplash.com/photo-1449300079323-02e209d9d3a6?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(117, 'https://images.unsplash.com/photo-1504544750208-dc0358e63f7f?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(118, 'https://images.unsplash.com/photo-1551754655-cd27e38d2076?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(119, 'https://images.unsplash.com/photo-1588879462716-16e03eaeb72e?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(120, 'https://images.unsplash.com/photo-1515471204630-e60b853e3042?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(121, 'https://images.unsplash.com/photo-1590483866160-5cf7b38cb09b?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(122, 'https://images.unsplash.com/photo-1571575173700-afb9492e6a50?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(123, 'https://images.unsplash.com/photo-1589135233689-d56d94f71120?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(124, 'https://images.unsplash.com/photo-1511688878353-3a2f5be94cd7?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(125, 'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(126, 'https://images.unsplash.com/photo-1593105544559-ecb03bf76f82?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(127, 'https://images.unsplash.com/photo-1519782772591-36455665e067?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(128, 'https://images.unsplash.com/photo-1595124245030-41448b199d6d?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(129, 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(130, 'https://images.unsplash.com/photo-1445282768818-728615cc910a?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(131, 'https://images.unsplash.com/photo-1618375569909-3c8616cf7733?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(132, 'https://images.unsplash.com/photo-1596797882870-8c33deeac224?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(133, 'https://images.unsplash.com/photo-1628556270448-4d4e4148e1b1?w=600&auto=format&fit=crop', 1, 1, 1, 0),

(201, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250103/brands/qlsbgtgno3kvconpo0c3.jpg', 1, 1, 1, 0),
(202, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250212/brands/mtbfbygr3tmlctmfdg0x.jpg', 1, 1, 1, 0),
(203, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917463/brands/xjz74sedvolg7woxywpo.jpg', 1, 1, 1, 0),
(204, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943219/brands/dysl261o6o2bapfwurmf.jpg', 1, 1, 1, 0),
(205, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917344/brands/c2i9gkbkl8vx7iwulwsz.png', 1, 1, 1, 0),
(206, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916481/brands/s5ld5232nl4s4fwczawr.jpg', 1, 1, 1, 0),
(207, 'https://images.unsplash.com/photo-1553279768-865429fa0078?w=600&h=400&fit=crop', 1, 1, 1, 0),
(208, 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917373/brands/h0mivuwgipa6hssaawfi.jpg', 1, 1, 1, 0),
(209, 'https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(210, 'https://images.unsplash.com/photo-1527325678964-54921661f888?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(211, 'https://images.unsplash.com/photo-1550258987-190a2d41a8ba?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(212, 'https://images.unsplash.com/photo-1594282486552-05b4d80fbb9f?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(213, 'https://images.unsplash.com/photo-1617112848923-cc2234396a8d?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(214, 'https://images.unsplash.com/photo-1536511135899-7921860086f6?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(215, 'https://images.unsplash.com/photo-1533082879395-3410c7d15bf4?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(216, 'https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(217, 'https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(218, 'https://images.unsplash.com/photo-1567375698348-5d9d5ae99de0?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(219, 'https://images.unsplash.com/photo-1588252303782-cb80119abd6d?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(220, 'https://images.unsplash.com/photo-1582281298055-e25b84a30b0b?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(221, 'https://images.unsplash.com/photo-1597714026732-3907092c7304?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(222, 'https://images.unsplash.com/photo-1592417817098-8f3d6910985c?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(223, 'https://images.unsplash.com/photo-1628088062854-d1870b4553da?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(224, 'https://images.unsplash.com/photo-1550258987-190a2d41a8ba?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(225, 'https://images.unsplash.com/photo-1615485500704-8e990f9900f7?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(226, 'https://images.unsplash.com/photo-1596040033229-a9821ebd058d?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(227, 'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(228, 'https://images.unsplash.com/photo-1567375698348-5d9d5ae99de0?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(229, 'https://images.unsplash.com/photo-1551754655-cd27e38d2076?w=600&auto=format&fit=crop', 1, 1, 1, 0),
(230, 'https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=600&auto=format&fit=crop', 1, 1, 1, 0)
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

-- 13. BLOGS (CÂU CHUYỆN NÔNG DÂN & BÀI VIẾT NHÀ VƯỜN)
INSERT INTO blogs (blog_id, shop_id, title, content, picture_url, status, category, views, create_at, updated_at) VALUES
(1, 4, 'Bí quyết canh tác rau hữu cơ sạch tại thung lũng Đà Lạt', 'Canh tác rau hữu cơ sạch không chỉ là một phương pháp trồng trọt mà còn là triết lý sống hòa hợp với thiên nhiên.\n\n1. Hiểu đất và nuôi dưỡng đất: Vườn chúng tôi ủ phân vi sinh từ vỏ cà phê, rơm rạ và chế phẩm sinh học EM trong 60 ngày để đất luôn tơi xốp, giàu mùn hữu cơ.\n2. Nguồn nước tưới tinh khiết: Nước suối đầu nguồn chảy qua hệ thống lắng lọc tự nhiên, kiểm định định kỳ không nhiễm kim loại nặng.\n3. Thiên địch thay thế thuốc trừ sâu: Trồng cúc vạn thọ xua đuổi tuyến trùng, nuôi bọ rùa và bọ ngựa bắt rệp sáp.\n\nNhờ vậy, từng bó rau xà lách, ngọn mồng tơi hay bắp cải gửi đến bàn ăn gia đình bạn đều giữ trọn độ giòn ngọt và an toàn tuyệt đối!', 'https://images.unsplash.com/photo-1595974482597-4b8da8879bc5?w=900&auto=format&fit=crop', 'PUBLISHED', 'NHA_NONG', 152, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

(2, 5, 'Mùa trái chín miền Tây: Nhật ký vườn sầu riêng & bưởi da xanh', 'Về Bến Tre và Tiền Giang những ngày vào vụ, hương bưởi da xanh thơm ngát khắp các rặng kênh mương.\n\nNăm nay thời tiết thuận lợi, phù sa bồi đắp dồi dào giúp múi bưởi hồng hào, tép mọng nước và ngọt lịm. Bà con nông dân thu hái bằng kéo chuyên dụng, bọc từng trái trong túi giấy chống côn trùng từ lúc còn nhỏ để không phải phun xịt hóa chất.\n\nCapNong đã trực tiếp liên kết với tổ hợp tác xã nhà vườn, thu mua tận gốc và phân loại nghiêm ngặt để đưa những trái đặc sản tươi ngon nhất đến tay người tiêu dùng thành phố trong vòng 24 giờ!', 'https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=900&auto=format&fit=crop', 'PUBLISHED', 'STORY', 230, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

(3, 4, 'Thực đơn thanh lọc detox 7 ngày từ rau củ quả tươi', 'Cơ thể mệt mỏi sau những ngày ăn uống nhiều dầu mỡ? Hãy cùng CapNong tái tạo năng lượng với thực đơn detox tự nhiên:\n\n- Sáng: Nước ép cần tây + táo xanh + gừng sẻ ấm bụng.\n- Trưa: Salad ớt chuông, dưa leo baby và cà chua bi hữu cơ cùng sốt mè rang.\n- Chiều: Sinh tố bơ sáp 034 dẻo mịn bổ sung chất béo lành mạnh.\n- Tối: Canh súp củ dền đỏ hầm bắp ngọt thanh mát.\n\nChỉ sau 1 tuần, bạn sẽ cảm nhận rõ làn da sáng khỏe, hệ tiêu hóa nhẹ nhàng và tinh thần tràn đầy sảng khoái!', 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=900&auto=format&fit=crop', 'PUBLISHED', 'SUC_KHOE', 315, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),

(4, 5, 'Bảo quản rau củ tươi lâu trong tủ lạnh - Mẹo vặt từ nhà nông', 'Nhiều gia đình mua rau về để tủ lạnh 2-3 ngày là héo úa hoặc úng nước. Sau đây là mẹo từ kinh nghiệm bảo quản của các bác nông dân:\n\n1. Không rửa rau trước khi cho vào tủ: Độ ẩm đọng lại trên lá là nguyên nhân hàng đầu khiến vi khuẩn phân hủy rau nhanh chóng. Chỉ rửa ngay trước khi nấu.\n2. Tách riêng chuối và cà chua: Chuối chín giải phóng khí ethylene kích thích các loại rau củ xung quanh chín ép và mau hỏng.\n3. Cà rốt và củ cải: Cắt bỏ phần cuống lá xanh ngay sau khi mua về để tránh lá hút ngược chất dinh dưỡng và nước từ củ.\n4. Bọc giấy thấm ẩm: Cho rau vào túi zip hoặc hộp nhựa có lót khăn giấy sạch bên dưới, giữ rau tươi giòn suốt cả tuần!', 'https://images.unsplash.com/photo-1542838132-92c53300491e?w=900&auto=format&fit=crop', 'PUBLISHED', 'CAM_NANG', 180, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

(5, 4, 'Hành trình giải cứu nông sản xấu mã - Biến cái "xấu" thành điều kỳ diệu', 'Tại sao một củ cà rốt có hai chân hay quả cà chua hơi cong vẹo lại bị bỏ rơi ngoài ruộng?\n\nThực tế, hình thức bên ngoài không hề quyết định giá trị dinh dưỡng bên trong. Những củ quả xấu mã lớn lên tự nhiên, không dùng thuốc kích thích tăng trưởng nên thường giòn ngọt và đượm vị hơn cả.\n\nCapNong ra đời với sứ mệnh đồng hành cùng người nông dân, trân quý từng giọt mồ hôi trên đồng ruộng và kết nối những nông sản thuần tự nhiên đến gian bếp của mỗi gia đình với mức giá tiết kiệm đến 40%!', 'https://images.unsplash.com/photo-1500937386664-56d1dfef3854?w=900&auto=format&fit=crop', 'PUBLISHED', 'TIN_TUC_CAPNONG', 420, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),

(6, 5, 'Xu hướng tiêu dùng xanh: Vì sao người trẻ chuộng nông sản địa phương?', 'Người tiêu dùng hiện đại ngày càng quan tâm đến nguồn gốc thực phẩm và dấu chân carbon (carbon footprint).\n\nLựa chọn nông sản địa phương mang lại 3 giá trị to lớn:\n1. Nông sản tươi ngon tối đa: Không qua các kho bảo quản lạnh hóa chất dài ngày.\n2. Hỗ trợ kinh tế gia đình nông dân: Tiền chi trả đến trực tiếp tay người trồng thay vì qua nhiều tầng trung gian.\n3. Bảo vệ môi trường: Giảm thiểu quãng đường vận chuyển và bao bì nhựa sử dụng một lần.\n\nHãy chung tay cùng CapNong xây dựng thói quen ăn sạch - sống xanh mỗi ngày!', 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=900&auto=format&fit=crop', 'PUBLISHED', 'XU_HUONG', 195, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),

(7, 4, 'Bí quyết làm sốt chấm chanh dây chua ngọt thần thánh', 'Chanh dây tím Đà Lạt đang vào mùa chín rộ mọng nước. Hãy thử ngay công thức sốt chấm chanh dây siêu bắt vị cho các món hải sản và đồ nướng:\n\n- Nguyên liệu: 4 quả chanh dây lọc hạt lấy cốt, 3 thìa đường cát, 1 thìa nước mắm ngon, 1 quả ớt sừng băm nhỏ, 1 tép tỏi nhuyễn.\n- Cách làm: Đun sôi nhẹ cốt chanh dây với đường cho tan sánh lại, tắt bếp để nguội rồi hòa cùng nước mắm, tỏi ớt.\n\nVị chua thanh kích thích vị giác kết hợp cùng vị ngọt dịu sẽ nâng tầm bất kỳ bữa cơm sum vầy nào!', 'https://images.unsplash.com/photo-1589135233689-d56d94f71120?w=900&auto=format&fit=crop', 'PUBLISHED', 'ME_O_VAT', 260, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),

(8, 5, 'Kinh nghiệm nhận biết nông sản VietGAP và nông sản hữu cơ', 'Rất nhiều người tiêu dùng còn nhầm lẫn giữa tiêu chuẩn VietGAP và Organic (Hữu cơ):\n\n- VietGAP: Cho phép sử dụng phân bón và thuốc BVTV trong danh mục được cấp phép, nhưng phải tuân thủ nghiêm ngặt thời gian cách ly trước thu hoạch để đảm bảo dư lượng bằng 0.\n- Organic: Tuyệt đối không sử dụng bất kỳ hóa chất tổng hợp, phân vô cơ, thuốc trừ sâu hóa học hay sinh vật biến đổi gen (Non-GMO).\n\nTại CapNong, từng lô nông sản đều có thông tin nhà vườn minh bạch, giấy chứng nhận được kiểm duyệt định kỳ để bảo vệ quyền lợi tối đa cho người tiêu dùng.', 'https://images.unsplash.com/photo-1592417817098-8f3d6910985c?w=900&auto=format&fit=crop', 'PUBLISHED', 'CAM_NANG', 310, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE title=VALUES(title), content=VALUES(content), picture_url=VALUES(picture_url), status=VALUES(status);

-- 14. BLOG LIKES & COMMENTS
INSERT INTO blog_likes (like_id, blog_id, user_id, create_at) VALUES
(1, 1, 2, NOW()),
(2, 1, 3, NOW()),
(3, 2, 2, NOW()),
(4, 3, 3, NOW()),
(5, 5, 2, NOW())
ON DUPLICATE KEY UPDATE create_at=VALUES(create_at);

INSERT INTO blog_comments (comment_id, blog_id, user_id, content, create_at) VALUES
(1, 1, 2, 'Bài viết rất hữu ích và chân thực! Nhà em mua rau xà lách của vườn ăn giòn ngọt lắm ạ.', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 1, 3, 'Cảm ơn anh Kiệt đã chia sẻ bí quyết nuôi đất. Ủng hộ nhà vườn tiếp tục làm nông nghiệp sạch!', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(3, 2, 2, 'Bưởi da xanh đợt này ngọt lịm không hạt, cả nhà ai cũng khen.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 3, 3, 'Mình đang áp dụng thực đơn detox này được 3 ngày, cảm giác nhẹ bụng và sảng khoái hẳn.', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 5, 2, 'Rất thích thông điệp giải cứu nông sản xấu mã của CapNong. Rau củ tuy vẻ ngoài hơi kỳ nhưng ăn ngon và an tâm!', DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE content=VALUES(content);

-- 15. NOTIFICATIONS (THÔNG BÁO MẪU CHO BUYER & HỆ THỐNG)
INSERT INTO notifications (noti_id, admin_id, title, message, evidence, create_at, receiver_type) VALUES
-- 1. Đơn hàng đã giao hoàn tất (Đơn #106 - Bưởi da xanh) -> Test deep-link vào /buyer/order/106
(1, 1, 'Đơn hàng #106 đã giao thành công', 'Đơn hàng #106 của bạn gồm Bưởi da xanh Bến Tre đã được giao hoàn tất bởi tài xế Nguyễn Văn Hoàng. Vui lòng kiểm tra và gửi đánh giá nông sản để tích điểm giải cứu nhé!', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917344/brands/c2i9gkbkl8vx7iwulwsz.png', DATE_SUB(NOW(), INTERVAL 20 MINUTE), 'BUYER'),

-- 2. Đang vận chuyển (Đơn #108 - Cà rốt & Rau muống) -> Test deep-link vào /buyer/order/108
(2, 1, 'Tài xế đang giao đơn hàng #108', 'Tài xế Nguyễn Văn Hoàng đang trên đường giao đơn hàng #108 đến bạn. Rau củ tươi được đóng thùng cách nhiệt bảo quản độ giòn mát tối đa, dự kiến giao trong 30 phút.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916721/brands/uj1eoinkwvthfjvtpvpq.jpg', DATE_SUB(NOW(), INTERVAL 1 HOUR), 'BUYER'),

-- 3. Xác nhận đơn hàng (Đơn #109 - Cà chua bi) -> Test deep-link vào /buyer/order/109
(3, 1, 'Đơn hàng #109 đã được xác nhận', 'Nhà vườn Nông Sản Xấu Mã - Vườn Đà Lạt đã xác nhận đơn hàng #109. Nông dân đang chuẩn bị thu hái Cà chua bi tươi ngon và đóng gói cẩn thận giao cho shipper.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916530/brands/xjiiaehe5evwybwhmpmu.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 'BUYER'),

-- 4. Đơn hàng chuẩn bị xuất kho (Đơn #112 - Xoài cát) -> Test deep-link vào /buyer/order/112
(4, 1, 'Đơn hàng #112 chuẩn bị xuất kho', 'Đơn hàng #112 gồm Xoài cát Hòa Lộc Tiền Giang ngọt lịm đã được nhà vườn Vườn Xanh tuyển chọn và đóng gói kỹ càng, chuẩn bị bàn giao cho đơn vị vận chuyển.', 'https://images.unsplash.com/photo-1553279768-865429fa0078?w=600&h=400&fit=crop', DATE_SUB(NOW(), INTERVAL 2 DAY), 'BUYER'),

-- 5. Voucher ưu đãi miễn phí ship -> Test deep-link vào /buyer/vouchers
(5, 1, 'Voucher mới: Giảm 15K phí vận chuyển', 'CapNong tặng bạn mã DALATFREESHIP giảm ngay 15.000đ phí giao hàng cho mọi đơn nông sản sạch từ 100.000đ. Nhấn để lưu voucher vào kho ưu đãi ngay!', NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR), 'BUYER'),

-- 6. Voucher ưu đãi giảm giá đơn hàng -> Test deep-link vào /buyer/vouchers
(6, 1, 'Voucher mới: CAPNONG10K tri ân khách', 'Mã ưu đãi CAPNONG10K giảm trực tiếp 10.000đ áp dụng cho mọi đơn hàng giải cứu rau củ quả tươi từ 50.000đ. Nhanh tay sử dụng trong 30 ngày nhé!', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), 'BUYER'),

-- 7. Nông sản mới về kho -> Icon Gói hàng (Package)
(7, 1, 'Nông sản mới: Dâu tây & Bơ sáp 034', 'Kho nông sản vừa cập bến lô Dâu tây giống Nhật đỏ mọng thu hái sáng sớm và Bơ sáp 034 Đắk Lắk dẻo béo ngậy. Mở app đặt ngay để giữ trọn vị tươi ngon!', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943061/brands/qtspypfmdjcxtsbme7xi.jpg', DATE_SUB(NOW(), INTERVAL 8 HOUR), 'BUYER'),

-- 8. Nông sản mới miền Tây -> Icon Gói hàng (Package)
(8, 1, 'Nông sản mới: Sầu riêng Ri6 & Bưởi', 'Vườn Xanh Miền Tây vừa mở bán đợt Sầu riêng Ri6 cơm vàng hạt lép chín cây và Bưởi da xanh tép hồng mọng nước chuẩn VietGAP với giá giải cứu siêu tiết kiệm.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917344/brands/c2i9gkbkl8vx7iwulwsz.png', DATE_SUB(NOW(), INTERVAL 4 DAY), 'BUYER'),

-- 9. Thông báo chào mừng thành viên mới -> Icon Chuông (Bell)
(9, 1, 'Chào mừng bạn đến với CapNong!', 'Chào mừng bạn gia nhập cộng đồng tiêu dùng xanh CapNong! Hãy cùng chúng tôi bảo vệ công sức người nông dân và thưởng thức nông sản thuần tự nhiên an toàn mỗi ngày.', 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg', DATE_SUB(NOW(), INTERVAL 5 DAY), 'BUYER'),

-- 10. Cập nhật tích điểm / xếp hạng giải cứu -> Icon Chuông (Bell)
(10, 1, 'Cập nhật hạng: Thành viên Nông Hộ', 'Chúc mừng bạn đã hoàn thành các đơn hàng giải cứu nông sản thành công và chính thức thăng hạng Thành viên Nông Hộ Thân Thiết. Nhiều ưu đãi đặc quyền đang chờ đón bạn!', NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), 'BUYER')
ON DUPLICATE KEY UPDATE title=VALUES(title), message=VALUES(message), evidence=VALUES(evidence), create_at=VALUES(create_at), receiver_type=VALUES(receiver_type);

-- 16. USER NOTIFICATIONS (LIÊN KẾT THÔNG BÁO VỚI BUYER)
-- Buyer 1 (id=2: Ngô Gia Sang / buyer1@gmail.com): 10 thông báo (4 chưa đọc: #1, #2, #5, #7; 6 đã đọc: #3, #4, #6, #8, #9, #10)
-- Buyer 2 (id=3: Trần Thị Mai / buyer2@gmail.com): 4 thông báo (2 chưa đọc: #5, #7; 2 đã đọc: #9, #10)
INSERT INTO user_notifications (id, user_id, notification_id, is_read, is_deleted) VALUES
(1, 2, 1, 0, 0),   -- Buyer 1: Chưa đọc (Đơn #106)
(2, 2, 2, 0, 0),   -- Buyer 1: Chưa đọc (Tài xế giao Đơn #108)
(3, 2, 5, 0, 0),   -- Buyer 1: Chưa đọc (Voucher 15K freeship)
(4, 2, 7, 0, 0),   -- Buyer 1: Chưa đọc (Nông sản mới Dâu tây & Bơ)
(5, 2, 3, 1, 0),   -- Buyer 1: Đã đọc (Đơn #109)
(6, 2, 4, 1, 0),   -- Buyer 1: Đã đọc (Đơn #112)
(7, 2, 6, 1, 0),   -- Buyer 1: Đã đọc (Voucher CAPNONG10K)
(8, 2, 8, 1, 0),   -- Buyer 1: Đã đọc (Nông sản mới Sầu riêng)
(9, 2, 9, 1, 0),   -- Buyer 1: Đã đọc (Chào mừng)
(10, 2, 10, 1, 0), -- Buyer 1: Đã đọc (Xếp hạng)

(11, 3, 5, 0, 0),  -- Buyer 2: Chưa đọc (Voucher freeship)
(12, 3, 7, 0, 0),  -- Buyer 2: Chưa đọc (Nông sản mới)
(13, 3, 9, 1, 0),  -- Buyer 2: Đã đọc (Chào mừng)
(14, 3, 10, 1, 0)  -- Buyer 2: Đã đọc (Xếp hạng)
ON DUPLICATE KEY UPDATE is_read=VALUES(is_read), is_deleted=VALUES(is_deleted);

SET FOREIGN_KEY_CHECKS = 1;
