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

-- 8. BLOGS & STORIES (PHÂN ĐỀU CHO SHOP 1 [ID: 4] VÀ SHOP 2 [ID: 5])
INSERT INTO blogs (blog_id, shop_id, title, content, picture_url, category, status, views, create_at, updated_at) VALUES
(1, 4, 'Cà rốt hai nhánh Cầu Đất: Đằng sau vẻ ngoài kỳ lạ là vị ngọt đậm tự nhiên hiếm có',
'<p>Tại các sườn đồi đất đỏ bazan thoai thoải ở Cầu Đất (Đà Lạt), mùa thu hoạch cà rốt luôn mang lại những bất ngờ thú vị cho người làm nông. Khác với những củ cà rốt thẳng tắp, bóng bẩy trên quầy siêu thị công nghiệp, những củ cà rốt nơi đây thường uốn lượn, thậm chí chẻ đôi thành hai nhánh như đôi chân của một vũ công nhỏ.</p>
<p>Lý giải về hiện tượng này, kỹ sư nông nghiệp Trần Văn Kiệt cho biết: "Đất canh tác hữu cơ của chúng tôi giàu mùn tự nhiên nhưng lẫn nhiều sỏi đá dung nham cổ xưa. Khi đầu rễ non đâm xuống gặp phải viên sỏi nhỏ, nó sẽ tự tách làm đôi để tiếp tục sinh trưởng. Vẻ ngoài có thể xấu mã, nhưng cấu trúc thịt củ lại đặc khít hơn, hàm lượng đường tự nhiên và hợp chất carotenoid cao hơn 15% so với củ trồng trên đất cát nhân tạo."</p>
<p>Khi ép lấy nước hoặc dùng nấu món canh sườn thanh mát, cà rốt hai nhánh cho nước cốt đậm đà, mùi thơm nồng nàn đặc trưng của đất trời cao nguyên. Đây là minh chứng rõ nét cho thông điệp: Vẻ đẹp đích thực của nông sản nằm ở sự tinh khiết và hương vị nguyên bản bên trong.</p>',
'https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=1200&q=80', 'STORY', 'PUBLISHED', 128, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

(2, 4, 'Hành trình 90 ngày canh tác dâu tây hữu cơ không màng thuốc bảo vệ thực vật tại cao nguyên',
'<p>Ở độ cao trên 1.500m so với mực nước biển, sương mù bao phủ những nhà màng trồng dâu tây sinh thái của nông trại chúng tôi suốt những sớm tinh mơ. Khác với mô hình canh tác thông thường sử dụng thuốc trừ nấm và hóa chất kích ngọt, chúng tôi chọn con đường khó khăn hơn: nuôi thiên địch bọ trĩ và xịt dịch chiết tỏi, ớt, gừng lên men tự nhiên.</p>
<p>Quá trình này đòi hỏi sự kiên nhẫn tột cùng. Trái dâu tây không dùng chất bảo quản thường có kích thước không đồng đều, có quả hơi méo mó ở phần cuống do thụ phấn tự nhiên bởi ong mật. Thế nhưng, khi cắn ngập chân răng, người sành ăn sẽ cảm nhận ngay độ giòn xốp, hậu vị ngọt thanh kèm chút chua dịu đánh thức mọi giác quan.</p>
<p>"Khách hàng hiện đại ngày càng hiểu rằng quả dâu hoàn hảo không tì vết thường phải trả giá bằng lượng hóa học tồn dư. Những quả dâu có chút khiếm khuyết ngoại hình nhưng an toàn tuyệt đối chính là món quà quý nhất mà đất mẹ ban tặng," đại diện nhà vườn chia sẻ.</p>',
'https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&w=1200&q=80', 'NONG_NGHIEP', 'PUBLISHED', 215, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

(3, 4, 'Giải cứu bắp cải Cầu Đất sau đợt sương muối: Khi sự chung tay thắp sáng niềm hy vọng',
'<p>Đợt rét đậm đầu tháng kéo theo sương muối dày đặc đã phủ trắng xóa các luống bắp cải đang bước vào kỳ thu hoạch rộ tại thung lũng Đơn Dương. Hàng ngàn búp cải bị cháy xém lớp lá ngoài cùng, khiến thương lái ép giá hoặc từ chối thu mua vì không đạt tiêu chuẩn trưng bày thông thường.</p>
<p>Tuy nhiên, chỉ cần khéo léo bóc tách 2 - 3 lớp bẹ lá ngoài bị sạm nám do sương giá, phần lõi bên trong bắp cải vẫn cuộn chắc nịch, giòn ngọt và căng mọng từng giọt nước mát lành. Nhờ sự hỗ trợ kết nối nhanh chóng từ nền tảng CapNong, hơn 3 tấn bắp cải đã được giải cứu thành công và đưa thẳng tới tay người tiêu dùng TP.HCM và các tỉnh lân cận chỉ trong vòng 48 giờ.</p>
<p>Sự đón nhận nồng nhiệt từ cộng đồng không chỉ giúp người làm vườn vượt qua tổn thất mùa vụ, mà còn chứng minh một tư duy tiêu dùng mới: ủng hộ giá trị thực tế của nông sản, giảm thiểu lãng phí thực phẩm cho xã hội.</p>',
'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=1200&q=80', 'NEW_PRODUCT', 'PUBLISHED', 342, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

(4, 4, 'Cẩm nang nhà bếp: 5 món canh thanh nhiệt tuyệt đỉnh từ củ quả xấu mã nhưng trọn vị ngọt',
'<p>Nhiều bà nội trợ thường có tâm lý e ngại khi thấy những củ quả hình dáng kỳ lạ, vỏ xù xì hoặc cong queo. Thực tế, trong nghệ thuật ẩm thực truyền thống, đây lại chính là những nguyên liệu vàng để tạo nên những nồi nước dùng ngọt nước, thanh mát tự nhiên mà không cần đến mì chính.</p>
<p>Dưới đây là 5 gợi ý tuyệt vời cho thực đơn gia đình trong mùa nắng nóng:
1. Canh sườn non hầm củ dền xoắn và khoai tây bi: Nước canh có màu hồng ngọc quyến rũ, vị ngọt bùi sâu lắng.
2. Súp bí đỏ hồ lô hạt sen: Những quả bí da sần sùi lại sở hữu lớp thịt dẻo quánh, hàm lượng vitamin A vượt trội.
3. Canh bí đao chanh tươi tôm nõn: Bí đao thụ phấn tự nhiên cho vị thanh mát giải nhiệt tức thì.
4. Nước ép cà rốt - cần tây - táo xanh: Sử dụng cà rốt nhánh nhỏ giữ trọn men vi sinh sống.
5. Canh chua cá lóc nấu cà chua bi nứt vỏ: Cà chua nứt vỏ khi chín rộ trên cây có độ chua ngọt hài hòa nhất.</p>
<p>Hãy cùng CapNong biến những nông sản mộc mạc thành những bữa cơm gia đình đầm ấm và ngập tràn tình yêu thương!</p>',
'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=1200&q=80', 'GUIDE', 'PUBLISHED', 89, DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR)),

(5, 5, 'Bưởi da xanh Bến Tre vỏ đốm nám: Vì sao các chuyên gia dinh dưỡng khuyên dùng?',
'<p>Dọc theo các con rạch trĩu nặng phù sa của huyện Châu Thành (Bến Tre), những vườn bưởi da xanh cổ thụ đang bước vào chính vụ. Mùa nắng gắt, dưới tác động của ánh sáng mặt trời nhiệt đới và sự tiếp xúc tự nhiên với nhện đỏ thân thiện, vỏ bưởi thường xuất hiện những mảng nám sậm màu (thường gọi là bưởi da cám).</p>
<p>Trái với định kiến của nhiều người rằng vỏ rám là bưởi sâu bệnh, các nhà khoa học thực phẩm khẳng định: Quá trình cây bưởi tự tiết chất bảo vệ khỏi ánh nắng gay gắt chính là cơ chế kích hoạt sản sinh chất chống oxy hóa tự nhiên flavonoids và naringin ở nồng độ cao nhất. Lớp vỏ nám bên ngoài đóng vai trò như một lớp áo giáp tự nhiên giữ cho từng tép bưởi hồng bên trong luôn mọng nước, tôm bưởi róc rách và vị ngọt thanh đậm đà.</p>
<p>Chọn mua bưởi da cám trực tiếp từ nhà vườn vừa tiết kiệm đến 35% chi phí so với hàng tuyển xuất khẩu, vừa mang lại nguồn vitamin C tinh khiết tuyệt vời cho sức khỏe cả gia đình.</p>',
'https://images.unsplash.com/photo-1619566636858-adf3ef46400b?auto=format&fit=crop&w=1200&q=80', 'SUC_KHOE', 'PUBLISHED', 194, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),

(6, 5, 'Chuyện cây ổi Nữ Hoàng đất phù sa: Giữ trọn sự mộc mạc giữa làn sóng công nghiệp hóa',
'<p>Trong căn chòi nhỏ bên bờ kênh rợp bóng dừa nước, dì Sáu Ngọc chia sẻ về vườn ổi Nữ Hoàng hơn 5 năm tuổi của mình. Giữa lúc nhiều nơi lạm dụng phân bón kích cỡ để ép quả ổi to đùng, bóng loáng như sáp nến, vườn của dì vẫn kiên định bón phân trùn quế và ủ bã đậu nành.</p>
<p>"Trái ổi của dì nó không tròn xoe, có trái méo mó, da hơi gồ ghề do cọ xát với cành lá lúc đón gió sông. Nhưng bổ ra thì hạt nhỏ xíu, cùi dày cui, giòn rụm và mùi thơm ngào ngạt lan khắp nhà," dì Ngọc cười rạng rỡ, đưa tay hái một trái ổi vừa chín tới còn đọng sương mai.</p>
<p>Câu chuyện của dì Sáu là nguồn cảm hứng lớn lao cho hành trình gìn giữ những giá trị nông nghiệp bền vững. Hãy thưởng thức trái cây với sự trân trọng mồ hôi công sức của người nông dân chân chính.</p>',
'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=1200&q=80', 'STORY', 'PUBLISHED', 167, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

(7, 5, 'Mẹo bảo quản rau ăn lá nhiệt đới tươi giòn suốt 7 ngày không cần túi nylon độc hại',
'<p>Rau ăn lá như cải ngọt, mồng tơi, rau dền rất dễ bị úa vàng hoặc nhớt lá chỉ sau 2 ngày để trong tủ lạnh nếu không biết cách xử lý độ ẩm dư thừa. Rất nhiều người có thói quen cột chặt rau trong túi nylon kín mít, điều này khiến khí ethylene do rau tự tiết ra bị nghẹt, đẩy nhanh tốc độ phân hủy.</p>
<p>Chuyên gia nông nghiệp từ Vườn Xanh Bình Dương chia sẻ bí quyết 3 bước cực kỳ đơn giản:
Bước 1: Không rửa rau trước khi cất tủ lạnh. Độ ẩm bề mặt là kẻ thù số một gây nấm mốc.
Bước 2: Dùng khăn bông ẩm hoặc giấy báo bọc lỏng quanh gốc rau, tạo điều kiện cho rau thở nhẹ nhàng.
Bước 3: Đặt rau đứng theo chiều phát triển tự nhiên trong ngăn chứa rau củ chuyên dụng (nhiệt độ lý tưởng từ 4 - 8 độ C).</p>
<p>Áp dụng cách này, bó rau của bạn sẽ giữ nguyên độ giòn xanh, lượng vitamin và khoáng chất quý giá suốt cả tuần lễ bận rộn!</p>',
'https://images.unsplash.com/photo-1518843875459-f738682238a6?auto=format&fit=crop&w=1200&q=80', 'GUIDE', 'PUBLISHED', 276, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

(8, 5, 'Ưu đãi mùa vụ: Thu hoạch xoài cát Hòa Lộc vỏ rám vàng - Ngọt lịm từng thớ thịt',
'<p>Xoài cát Hòa Lộc từ lâu đã được mệnh danh là vương hậu của các loại cây ăn trái miền Tây Nam Bộ. Năm nay, do thời tiết chuyển mùa hanh khô, những chùm xoài đón trọn vẹn ánh nắng hướng đông xuất hiện lớp da rám nắng lấm tấm chấm mật quanh cuống.</p>
<p>Nhiều thương lái bán chợ truyền thống chê xoài rám vỏ, nhưng chính người sành ăn xứ Tiền Giang đều biết: Chỉ những quả xoài dãi dầu sương nắng đủ ngày đủ tháng mới tích tụ trọn vẹn lượng mật ngọt sánh mịn, thơm lừng mùi vani nhiệt đới, dốc hạt mỏng dính và không hề có xơ.</p>
<p>Nhân dịp thu hoạch rộ, nhà vườn gửi tặng quý khách hàng CapNong chương trình ưu đãi giảm 20% khi đặt mua từ 5kg xoài cát Hòa Lộc da rám mật ong. Hãy trải nghiệm ngay hương vị ngọt ngào tuyệt hảo đậm đà phù sa miền sông nước!</p>',
'https://images.unsplash.com/photo-1553279768-865429fa0078?auto=format&fit=crop&w=1200&q=80', 'PROMOTION', 'PUBLISHED', 310, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR))
ON DUPLICATE KEY UPDATE title=VALUES(title), content=VALUES(content), picture_url=VALUES(picture_url), category=VALUES(category);

-- Sample blog likes & comments
INSERT INTO blog_likes (like_id, blog_id, user_id, create_at) VALUES
(1, 1, 2, NOW()),
(2, 5, 4, NOW())
ON DUPLICATE KEY UPDATE blog_id=VALUES(blog_id);

INSERT INTO blog_comments (comment_id, blog_id, user_id, content, create_at) VALUES
(1, 1, 2, 'Cà rốt này nhà mình mua về ép nước với táo, ngọt lịm tự nhiên không cần thêm đường luôn! Rất cảm ơn các cô chú nông dân.', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 1, 4, 'Dạ vườn cảm ơn gia đình mình nhiều ạ! Vụ sau vườn sẽ tiếp tục gửi đến bà con những mẻ rau củ sạch nhất nhé 🌱', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(3, 5, 2, 'Bưởi da cám ăn thơm và đậm vị hơn hẳn bưởi vỏ xanh mướt bày siêu thị. Cả nhà mình ai cũng mê!', DATE_SUB(NOW(), INTERVAL 6 HOUR))
ON DUPLICATE KEY UPDATE content=VALUES(content);

SET FOREIGN_KEY_CHECKS = 1;
