
USE db_capnong;

-- 1. Cập nhật Campaign 3: trạng thái FULFILLING, số liệu chính xác
UPDATE campaigns SET
    status = 'FULFILLING',
    booked_volume_kg = 1250.00,
    target_volume_kg = 3000.00,
    min_threshold_kg = 800.00,
    rescue_price_per_kg = 12000.00,
    deposit_percent = 0.3000,
    collection_site_name = 'Kho điều phối Vĩnh Long Hub',
    collection_site_address = 'Đường Phạm Hùng, Phường 9, TP. Vĩnh Long',
    cutoff_at = '2026-11-20 20:00:00',
    delivery_date = '2026-11-25',
    call_posted_at = '2026-11-10 08:00:00',
    published_at = '2026-11-12 08:00:00',
    evaluated_at = '2026-11-20 20:05:00',
    banner_badge = 'ĐANG GOM HÀNG',
    banner_cta_text = 'THEO DÕI ĐƠN',
    banner_headline = 'Cam Sành Vĩnh Long - Đã đạt 1.250kg cam kết!',
    banner_subline = 'Chiến dịch đã đủ ngưỡng và đang gom hàng tại điểm tập kết Vĩnh Long.',
    banner_theme = 'EMERALD',
    banner_on_home = 1
WHERE campaign_id = 3;

-- 2. Thêm các lô Nhà Vườn đăng ký (campaign_farm_registrations)
DELETE FROM campaign_farm_registrations WHERE campaign_id = 3;

INSERT INTO campaign_farm_registrations (
    registration_id, campaign_id, shop_id, product_name, shelf_life_tier,
    surplus_volume_kg, allocated_volume_kg, received_weight_kg, rejected_weight_kg, accepted_weight_kg,
    quality_passed, proposed_price_per_kg, agreed_price_per_kg, harvest_date,
    status, note, intake_note, intake_photo_urls, intake_at, created_at, updated_at
) VALUES 
(11, 3, 4, 'Cam sành Tam Bình loại 1', 'DAYS_5_6',
 800.00, 750.00, 760.00, 10.00, 750.00,
 1, 10500.00, 10000.00, '2026-11-20',
 'APPROVED', 'Cam sành cành lá xanh tươi, thu hoạch sáng sớm tại vườn xã Bình Ninh.',
 'Đã đo độ brix 11.5, loại bỏ 10kg quả trầy xước vỏ, 750kg đạt chuẩn nhập kho.',
 'https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?auto=format&fit=crop&w=600&q=80',
 '2026-11-22 09:30:00', '2026-11-15 10:00:00', NOW()),

(12, 3, 5, 'Cam sành hữu cơ Tam Bình', 'DAYS_5_6',
 600.00, 500.00, 510.00, 10.00, 500.00,
 1, 10500.00, 10000.00, '2026-11-20',
 'APPROVED', 'Cam sành canh tác hữu cơ VietGAP, quả đều da bóng mọng nước.',
 'Đạt chuẩn đóng sọt, quả căng mọng mỏng vỏ, nhập kho điều phối 500kg.',
 'https://images.unsplash.com/photo-1582979512210-99b6a53386f9?auto=format&fit=crop&w=600&q=80',
 '2026-11-22 10:15:00', '2026-11-16 14:00:00', NOW()),

(13, 3, 4, 'Cam sành Tam Bình (Lô bổ sung)', 'DAYS_5_6',
 200.00, NULL, NULL, NULL, NULL,
 NULL, 10000.00, 10000.00, '2026-11-26',
 'APPROVED', 'Nhà vườn đăng ký thêm 200kg sẵn sàng chi viện nếu các sọt bị dập thiếu hụt.',
 NULL, NULL, NULL, '2026-11-21 08:30:00', NOW());

-- 3. Xoá đơn đặt trước cũ của campaign 3 (nếu có) và tạo mới
DELETE FROM pre_order_damage_claims WHERE pre_order_id IN (SELECT pre_order_id FROM pre_orders WHERE campaign_id = 3);
DELETE FROM campaign_deposits WHERE pre_order_id IN (SELECT pre_order_id FROM pre_orders WHERE campaign_id = 3);
DELETE FROM pre_orders WHERE campaign_id = 3;

-- Đơn 1: 5 thùng 10kg = 50kg (PROCESSING - Đã đóng gói)
INSERT INTO pre_orders (
    pre_order_id, code, campaign_id, buyer_id, crate_size, crate_quantity, total_kg,
    unit_price, total_amount, deposit_amount, remaining_amount,
    recipient_name, recipient_phone, shipping_address, delivery_ward, delivery_zone,
    fulfillment_method, shipping_distance_km, shipping_base_fee, shipping_fee,
    status, qr_code, source_registration_id, deposit_paid_at, packed_at, created_at, updated_at
) VALUES (
    101, 'PO2611200001', 3, 2, 'KG_10', 5, 50.00,
    12000.00, 600000.00, 180000.00, 420000.00,
    'Ngô Gia Sang', '0901234567', '128 Hai Bà Trưng, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh', 'Phường Đa Kao', 'Khu vực 1 - Q1',
    'DELIVERY', 8.5, 35000.00, 35000.00,
    'PROCESSING', 'CAPNONG-PO2611200001-QR', 11, '2026-11-18 10:30:00', '2026-11-23 09:00:00', '2026-11-18 10:15:00', NOW()
);

-- Đơn 2: 10 thùng 20kg = 200kg (READY - Tại Hub, chờ shipper)
INSERT INTO pre_orders (
    pre_order_id, code, campaign_id, buyer_id, crate_size, crate_quantity, total_kg,
    unit_price, total_amount, deposit_amount, remaining_amount,
    recipient_name, recipient_phone, shipping_address, delivery_ward, delivery_zone,
    fulfillment_method, shipping_distance_km, shipping_base_fee, shipping_fee,
    status, qr_code, source_registration_id, deposit_paid_at, packed_at, ready_at, created_at, updated_at
) VALUES (
    102, 'PO2611200002', 3, 3, 'KG_20', 10, 200.00,
    12000.00, 2400000.00, 720000.00, 1680000.00,
    'Trần Thị Mai (Bếp Cơm Xanh)', '0912345678', '45 Lê Văn Việt, Phường Hiệp Phú, TP. Thủ Đức, TP. Hồ Chí Minh', 'Phường Hiệp Phú', 'Khu vực 2 - Thủ Đức',
    'DELIVERY', 15.0, 50000.00, 50000.00,
    'READY', 'CAPNONG-PO2611200002-QR', 11, '2026-11-18 14:00:00', '2026-11-23 10:00:00', '2026-11-24 07:30:00', '2026-11-18 13:45:00', NOW()
);

-- Đơn 3: 20 thùng 15kg = 300kg (PROCESSING - Tự đến kho lấy)
INSERT INTO pre_orders (
    pre_order_id, code, campaign_id, buyer_id, crate_size, crate_quantity, total_kg,
    unit_price, total_amount, deposit_amount, remaining_amount,
    recipient_name, recipient_phone, shipping_address, delivery_ward, delivery_zone,
    fulfillment_method, shipping_distance_km, shipping_base_fee, shipping_fee,
    status, qr_code, source_registration_id, deposit_paid_at, packed_at, created_at, updated_at
) VALUES (
    103, 'PO2611200003', 3, 2, 'KG_15', 20, 300.00,
    12000.00, 3600000.00, 1080000.00, 2520000.00,
    'Cửa Hàng Nước Ép Tươi', '0909888999', 'Kho điều phối Vĩnh Long Hub, Đường Phạm Hùng, Phường 9', 'Phường 9', 'Hub Vĩnh Long',
    'PICKUP', 0.0, 0.00, 0.00,
    'PROCESSING', 'CAPNONG-PO2611200003-QR', 12, '2026-11-19 09:15:00', '2026-11-23 11:30:00', '2026-11-19 09:00:00', NOW()
);

-- Đơn 4: 20 thùng 10kg = 200kg (SHIPPING - Shipper đang giao)
INSERT INTO pre_orders (
    pre_order_id, code, campaign_id, buyer_id, crate_size, crate_quantity, total_kg,
    unit_price, total_amount, deposit_amount, remaining_amount,
    recipient_name, recipient_phone, shipping_address, delivery_ward, delivery_zone,
    fulfillment_method, shipping_distance_km, shipping_base_fee, shipping_fee,
    status, qr_code, shipper_id, source_registration_id, deposit_paid_at, packed_at, ready_at, picked_up_at, created_at, updated_at
) VALUES (
    104, 'PO2611200004', 3, 3, 'KG_10', 20, 200.00,
    12000.00, 2400000.00, 720000.00, 1680000.00,
    'Chị Lan Anh', '0988111222', '72 Nguyễn Thị Minh Khai, Phường 6, Quận 3, TP. Hồ Chí Minh', 'Phường 6', 'Khu vực 1 - Q3',
    'DELIVERY', 10.2, 40000.00, 40000.00,
    'SHIPPING', 'CAPNONG-PO2611200004-QR', 6, 12, '2026-11-19 15:30:00', '2026-11-23 14:00:00', '2026-11-24 08:00:00', '2026-11-24 09:30:00', '2026-11-19 15:10:00', NOW()
);

-- Đơn 5: 40 thùng 5kg = 200kg (DEPOSIT_PAID - Đã cọc, chờ đóng gói)
INSERT INTO pre_orders (
    pre_order_id, code, campaign_id, buyer_id, crate_size, crate_quantity, total_kg,
    unit_price, total_amount, deposit_amount, remaining_amount,
    recipient_name, recipient_phone, shipping_address, delivery_ward, delivery_zone,
    fulfillment_method, shipping_distance_km, shipping_base_fee, shipping_fee,
    status, deposit_paid_at, created_at, updated_at
) VALUES (
    105, 'PO2611200005', 3, 2, 'KG_5', 40, 200.00,
    12000.00, 2400000.00, 720000.00, 1680000.00,
    'Nhóm Cư Dân Vinhomes', '0977665544', 'Toà Landmark 3, Vinhomes Central Park, Phường 22, Bình Thạnh', 'Phường 22', 'Khu vực 3 - Bình Thạnh',
    'DELIVERY', 12.0, 45000.00, 45000.00,
    'DEPOSIT_PAID', '2026-11-20 11:00:00', '2026-11-20 10:45:00', NOW()
);

-- Đơn 6: 15 thùng 20kg = 300kg (DELIVERED - Đã giao thành công)
INSERT INTO pre_orders (
    pre_order_id, code, campaign_id, buyer_id, crate_size, crate_quantity, total_kg,
    unit_price, total_amount, deposit_amount, remaining_amount,
    recipient_name, recipient_phone, shipping_address, delivery_ward, delivery_zone,
    fulfillment_method, shipping_distance_km, shipping_base_fee, shipping_fee,
    status, qr_code, shipper_id, source_registration_id, deposit_paid_at, packed_at, ready_at, picked_up_at, delivered_at, created_at, updated_at
) VALUES (
    106, 'PO2611200006', 3, 3, 'KG_20', 15, 300.00,
    12000.00, 3600000.00, 1080000.00, 2520000.00,
    'Bếp Cơm Thiện Tâm', '0933222111', '15/3 Hoàng Hoa Thám, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh', 'Phường 13', 'Khu vực 4 - Tân Bình',
    'DELIVERY', 14.5, 50000.00, 50000.00,
    'DELIVERED', 'CAPNONG-PO2611200006-QR', 6, 11, '2026-11-18 16:20:00', '2026-11-22 15:00:00', '2026-11-23 08:30:00', '2026-11-23 10:00:00', '2026-11-23 11:45:00', '2026-11-18 16:00:00', NOW()
);

-- 4. Tạo khoản cọc tương ứng trong campaign_deposits
INSERT INTO campaign_deposits (deposit_id, pre_order_id, amount, status, held_at, created_at, updated_at) VALUES
(101, 101, 180000.00, 'HELD_IN_ESCROW', '2026-11-18 10:30:00', NOW(), NOW()),
(102, 102, 720000.00, 'HELD_IN_ESCROW', '2026-11-18 14:00:00', NOW(), NOW()),
(103, 103, 1080000.00, 'HELD_IN_ESCROW', '2026-11-19 09:15:00', NOW(), NOW()),
(104, 104, 720000.00, 'HELD_IN_ESCROW', '2026-11-19 15:30:00', NOW(), NOW()),
(105, 105, 720000.00, 'HELD_IN_ESCROW', '2026-11-20 11:00:00', NOW(), NOW()),
(106, 106, 1080000.00, 'RELEASED_TO_FARMER', '2026-11-18 16:20:00', NOW(), NOW());

-- 5. Tạo 1 khiếu nại báo hư mẫu (pre_order_damage_claims) cho đơn 106
INSERT INTO pre_order_damage_claims (
    claim_id, pre_order_id, buyer_id, status, claimed_kg,
    photo_urls, reason, created_at
) VALUES (
    101, 106, 3, 'PENDING', 4.50,
    'https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?auto=format&fit=crop&w=600&q=80',
    'Thùng cam nhận lúc trưa có khoảng 4.5kg quả dưới đáy bị dập nát do chèn ép trên đường vận chuyển.',
    NOW() - INTERVAL 2 HOUR
);
