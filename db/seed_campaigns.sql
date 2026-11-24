
USE db_capnong;

-- Insert Campaign 2: Dưa hấu Long An (ACTIVE)
INSERT INTO campaigns (
    campaign_id, code, title, description, category_id, season, cover_image_url,
    target_volume_kg, min_threshold_kg, booked_volume_kg, rescue_price_per_kg, deposit_percent,
    status, collection_site_name, collection_site_address, cutoff_at, delivery_date,
    created_by, banner_badge, banner_cta_text, banner_headline, banner_subline,
    banner_theme, banner_on_home, pickup_enabled, ship_free_kg, ship_half_kg, ship_free_radius_km,
    created_at, updated_at, published_at
) VALUES (
    2, 'CD2026112401', 'Chiến dịch Giải cứu Dưa hấu Long An',
    '<p>Hỗ trợ bà con nông dân Long An tiêu thụ dưa hấu ruột đỏ ngọt lịm bị ứ đọng do biến động thương lái. Nông sản đạt chuẩn VietGAP, quả tròn mọng nước, độ ngọt thanh.</p>',
    3, 'Vụ Đông Xuân 2026', 'https://images.unsplash.com/photo-1587049352846-4a222e784d38?auto=format&fit=crop&w=800&q=80',
    2000.00, 500.00, 420.00, 9500.00, 0.3000,
    'ACTIVE', 'Điểm tập kết Nông sản Huyện Bến Lức', 'Quốc lộ 1A, Thị trấn Bến Lức, Tỉnh Long An',
    '2026-12-10 18:00:00', '2026-12-12',
    1, 'CHỈ CỌC 30%', 'ĐẶT TRƯỚC NGAY', 'Giải cứu Dưa hấu Long An - Ngọt mát chuẩn VietGAP',
    'Đã đạt 420kg / 500kg ngưỡng tối thiểu. Đặt ngay để chung tay cùng nhà vườn!',
    'ORANGE', 1, 1, 30.00, 15.00, 10,
    NOW(), NOW(), NOW()
) ON DUPLICATE KEY UPDATE title=VALUES(title), status=VALUES(status), booked_volume_kg=VALUES(booked_volume_kg);

-- Insert Campaign 3: Cam sành Vĩnh Long (THRESHOLD_MET)
INSERT INTO campaigns (
    campaign_id, code, title, description, category_id, season, cover_image_url,
    target_volume_kg, min_threshold_kg, booked_volume_kg, rescue_price_per_kg, deposit_percent,
    status, collection_site_name, collection_site_address, cutoff_at, delivery_date,
    created_by, banner_badge, banner_cta_text, banner_headline, banner_subline,
    banner_theme, banner_on_home, pickup_enabled,
    created_at, updated_at, published_at
) VALUES (
    3, 'CD2026112402', 'Chiến dịch Giải cứu Cam Sành Vĩnh Long',
    '<p>Cam sành Tam Bình mọng nước, vỏ mỏng tép vàng tươi ngon, thu hoạch trực tiếp tại vườn của các hộ nông dân xã Bình Ninh.</p>',
    3, 'Vụ Thu Đông 2026', 'https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?auto=format&fit=crop&w=800&q=80',
    3000.00, 800.00, 1250.00, 12000.00, 0.3000,
    'THRESHOLD_MET', 'Kho điều phối Vĩnh Long Hub', 'Đường Phạm Hùng, Phường 9, TP. Vĩnh Long',
    '2026-11-30 20:00:00', '2026-12-02',
    1, 'ĐÃ ĐỦ NGƯỠNG', 'XEM TIẾN ĐỘ', 'Cam Sành Vĩnh Long - Đã đạt 1.250kg cam kết!',
    'Chiến dịch đã đủ ngưỡng thực hiện. Chuẩn bị thu hoạch và gom hàng giao khách.',
    'EMERALD', 1, 1,
    NOW(), NOW(), NOW()
) ON DUPLICATE KEY UPDATE title=VALUES(title), status=VALUES(status), booked_volume_kg=VALUES(booked_volume_kg);

-- Insert Proposal from Nhà Vườn 1 (shop 4) - Status: SUBMITTED (Chờ duyệt)
INSERT INTO campaign_proposals (
    proposal_id, code, initiator_shop_id, category_id, product_name, shelf_life_tier,
    market_price_per_kg, desired_price_per_kg, harvest_from, harvest_to,
    status, story, site_name, site_address, photo_urls,
    created_at, updated_at, submitted_at
) VALUES (
    2, 'DX2026112401', 4, 2, 'Bắp cải tím Đà Lạt', 'DAYS_5_6',
    22000.00, 13500.00, '2026-12-01', '2026-12-10',
    'SUBMITTED', 'Vườn bắp cải tím 1.5 hecta tại Lạc Dương chuẩn bị vào vụ rộ, sản lượng ước tính hơn 3 tấn cần CapNong hỗ trợ mở chiến dịch giải cứu để tránh cảnh đổ bỏ.',
    'Hợp tác xã Nông sản sạch Lạc Dương', 'Thị trấn Lạc Dương, Huyện Lạc Dương, Tỉnh Lâm Đồng',
    'https://images.unsplash.com/photo-1596797038530-2c107229654b?auto=format&fit=crop&w=800&q=80',
    NOW(), NOW(), NOW()
) ON DUPLICATE KEY UPDATE status=VALUES(status);

-- Insert Farm Registrations for Campaign 2
INSERT INTO campaign_farm_registrations (
    registration_id, campaign_id, shop_id, surplus_volume_kg, accepted_weight_kg,
    agreed_price_per_kg, harvest_date, status, product_name, shelf_life_tier, created_at, updated_at
) VALUES 
(1, 2, 4, 300.00, 300.00, 9500.00, '2026-12-10', 'APPROVED', 'Dưa hấu Long An', 'DAYS_5_6', NOW(), NOW()),
(2, 2, 5, 250.00, 200.00, 9500.00, '2026-12-10', 'APPROVED', 'Dưa hấu Long An', 'DAYS_5_6', NOW(), NOW())
ON DUPLICATE KEY UPDATE status=VALUES(status);
