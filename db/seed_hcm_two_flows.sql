-- ==============================================================================
-- CAPNONG - DỮ LIỆU TEST TẠI TP. HỒ CHÍ MINH CHO 2 LUỒNG TẠO CHIẾN DỊCH (seed_hcm_two_flows.sql)
-- Dựa trên sep_minimal.sql (cùng 4 vai trò, danh mục và mật khẩu), thêm hub, shop lân cận, người mua, tài xế.
--
-- !!! CẢNH BÁO: file này XOÁ HẲN database DB_CapNong (mọi bảng, mọi dữ liệu) rồi tạo lại từ đầu.
-- Muốn giữ dữ liệu cũ thì xuất ra trước (MySQL Workbench > Server > Data Export).
--
-- Cách dùng (không cần chạy sep_minimal.sql hay file nào khác)
--   1. TẮT BE (BE đang chạy sẽ giữ kết nối làm lệnh xoá database bị treo).
--   2. Chạy cả file này trong MySQL Workbench. Ngày tháng tính theo hôm chạy, muốn làm mới thì chạy lại.
--   3. Bật BE, rồi đăng xuất và đăng nhập lại trên web / app.
--
-- Tài khoản (mật khẩu: Admin@123)
--   admin@gmail.com
--   shop1..shop6@gmail.com    cụm Hóc Môn - Củ Chi - Bình Chánh (cách điểm tập kết Hóc Môn dưới 10 km)
--   shop7..shop11@gmail.com   cụm Thủ Đức (quanh 20 Võ Văn Ngân)
--   shop12@gmail.com          shop tạm ngưng: không được hiện trong danh sách shop lân cận
--   buyer1..buyer10@gmail.com người mua, ví CapNong có 20.000.000đ để cọc không cần PayOS
--   taixe1..taixe9@gmail.com  tài xế xe ra hub, rải quanh điểm tập kết để thử bán kính:
--       trong 3 km: taixe1 (1 tấn), taixe2 (bán tải), taixe3 (1 tấn)
--       3 - 5 km: taixe4 (2 tấn) · 5 - 10 km: taixe5 (2 tấn), taixe6 (5 tấn)
--       10 - 20 km: taixe7 (5 tấn), taixe8 (1 tấn) · trên 20 km: taixe9 (5 tấn, chỉ hiện khi chọn "Tất cả")
--   giaohang1..4@gmail.com    shipper giao tận nhà (giaohang4 đi xe máy, chở tối đa 60 kg)
--
-- Đơn TEST-HV-PO-20 (Long Thành, 60 kg) xa mọi hub hơn 20 km nên CHƯA chia hub: còn đơn này thì chưa gọi xe được.
-- Mở tab Bản đồ hub để thêm hub mới ở Long Thành, hoặc gán đơn vào một hub có sẵn, rồi đổi cỡ xe để so số xe.
--
-- Luồng 1 - Admin tạo chiến dịch
--   admin > Chiến dịch giải cứu > Tạo chiến dịch (điểm tập kết gõ "Hóc Môn" hoặc "Võ Văn Ngân")
--   > Mời shop lân cận > shop đăng ký số kg > admin duyệt > Mở bán > buyer đặt đơn.
-- Luồng 2 - Shop tạo chiến dịch
--   shop1 > Kênh Nhà Vườn > đề xuất chiến dịch (thu hoạch từ ít nhất 3 ngày nữa) > mời shop lân cận
--   > shop được mời đồng ý số kg (tổng từ 200 kg) > gửi duyệt > admin duyệt và đăng chiến dịch.
-- Chiến dịch có sẵn "TEST-HV-01" Khoai lang mật: đã chốt đơn, HÔM NAY mở điểm tập kết (cũng là ngày cuối),
--   2.890 kg / 21 đơn. Làm tiếp: admin Dự toán & gọi xe > taixe nhận > Gom hàng > cân 4 lô > đóng gói
--   > Chốt ra hub > taixe nhận thùng, chụp ảnh, giao từng hub > admin xác nhận tại hub > giaohang nhận và giao.
-- ==============================================================================

DROP DATABASE IF EXISTS DB_CapNong;
CREATE DATABASE DB_CapNong CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE DB_CapNong;

SET NAMES utf8mb4;

-- ------------------------------------------------------------------------------
-- A. CẤU TRÚC BẢNG (sinh từ các entity của BE, khớp code hiện tại)
-- ------------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

create table allergens (id integer not null auto_increment, name varchar(100) not null, primary key (id)) engine=InnoDB;
create table blog_comments (blog_id integer not null, comment_id integer not null auto_increment, user_id integer not null, create_at datetime(6), content TEXT not null, primary key (comment_id)) engine=InnoDB;
create table blog_likes (blog_id integer not null, like_id integer not null auto_increment, user_id integer not null, create_at datetime(6), primary key (like_id)) engine=InnoDB;
create table blogs (blog_id integer not null auto_increment, product_id integer, shop_id integer not null, views integer, create_at datetime(6), updated_at datetime(6), picture_url varchar(500), content TEXT, title varchar(255) not null, category enum ('CAM_NANG','GUIDE','KHUYEN_MAI','ME_O_VAT','NEW_PRODUCT','NHA_NONG','NONG_NGHIEP','PROMOTION','STORY','SUC_KHOE','TIN_TUC_CAPNONG','XU_HUONG'), status enum ('DRAFT','HIDDEN','PUBLISHED') not null, primary key (blog_id)) engine=InnoDB;
create table bot_knowledge (active bit, id integer not null auto_increment, created_at datetime(6), updated_at datetime(6), content TEXT, keywords TEXT, title varchar(255), type varchar(255), primary key (id)) engine=InnoDB;
create table build_combos (combo_id integer not null auto_increment, price decimal(38,2), shop_id integer not null, sold_quantity integer, total_quantity integer, status varchar(50), cooking_suggestion varchar(500), description varchar(500), image_url varchar(500), combo_name varchar(255) not null, region varchar(255), type varchar(255), meal_type enum ('BREAKFAST','DINNER','LUNCH','SNACK'), primary key (combo_id)) engine=InnoDB;
create table build_plans (buyer_id integer not null, number_of_days integer, number_of_people integer, plan_id integer not null auto_increment, target_budget decimal(38,2), created_at datetime(6), updated_at datetime(6), meal_type varchar(255), plan_name varchar(255), source enum ('AI','MANUAL'), status enum ('AI_DRAFT','ARCHIVED','CONFIRMED','DRAFT'), primary key (plan_id)) engine=InnoDB;
create table campaign_deposits (amount decimal(15,2) not null, deposit_id integer not null auto_increment, pre_order_id integer not null, wallet_amount decimal(38,2), created_at datetime(6), held_at datetime(6), refunded_at datetime(6), released_at datetime(6), updated_at datetime(6), wallet_refunded_at datetime(6), payos_order_code varchar(30), status varchar(30) not null, checkout_url TEXT, qr_code_url TEXT, primary key (deposit_id)) engine=InnoDB;
create table campaign_farm_registrations (accepted_weight_kg decimal(12,2), agreed_price_per_kg decimal(12,2), allocated_volume_kg decimal(12,2), campaign_id integer not null, harvest_date date not null, payout_amount decimal(15,2), price_committed bit, product_id integer, proposed_price_per_kg decimal(12,2), quality_passed bit, received_weight_kg decimal(12,2), registration_id integer not null auto_increment, rejected_weight_kg decimal(12,2), shop_id integer not null, supplement_kg decimal(12,2), surplus_volume_kg decimal(12,2) not null, created_at datetime(6), intake_at datetime(6), paid_out_at datetime(6), reviewed_at datetime(6), updated_at datetime(6), shelf_life_tier varchar(20), status varchar(20) not null, product_name varchar(200), intake_note varchar(500), note varchar(500), review_note varchar(500), intake_photo_urls TEXT, product_image_urls TEXT, primary key (registration_id)) engine=InnoDB;
create table campaign_proposal_members (distance_km float(53), estimated_kg decimal(15,2), member_id integer not null auto_increment, proposal_id integer not null, shop_id integer not null, expires_at datetime(6), invited_at datetime(6), responded_at datetime(6), role varchar(20) not null, status varchar(20) not null, message varchar(300), primary key (member_id)) engine=InnoDB;
create table campaign_proposals (campaign_id integer, category_id integer, desired_price_per_kg decimal(15,2), harvest_from date, harvest_to date, initiator_shop_id integer not null, market_price_per_kg decimal(15,2), proposal_id integer not null auto_increment, proposed_close_date date, proposed_open_date date, reviewed_by integer, site_latitude float(53), site_longitude float(53), created_at datetime(6), reviewed_at datetime(6), submitted_at datetime(6), updated_at datetime(6), shelf_life_tier varchar(20), status varchar(20) not null, code varchar(32) not null, product_name varchar(200), site_name varchar(200), admin_note varchar(500), site_address varchar(500), photo_urls TEXT, story TEXT, primary key (proposal_id)) engine=InnoDB;
create table campaigns (banner_on_home bit, booked_volume_kg decimal(12,2) not null, campaign_id integer not null auto_increment, category_id integer, collection_site_latitude float(53), collection_site_longitude float(53), created_by integer, delivery_date date, deposit_percent decimal(5,4) not null, end_date date, finish_date date, home_slide_order integer, home_slide_seconds integer, min_threshold_kg decimal(12,2) not null, pickup_enabled bit, rescue_price_per_kg decimal(15,2) not null, shipper_call_trips integer, start_date date, target_volume_kg decimal(12,2) not null, top_up_kg decimal(12,2), auto_publish_failed_at datetime(6), banner_updated_at datetime(6), call_posted_at datetime(6), cancelled_at datetime(6), completed_at datetime(6), created_at datetime(6), cutoff_at datetime(6) not null, evaluated_at datetime(6), farm_cutoff_at datetime(6), farm_cutoff_checked_at datetime(6), publish_at datetime(6), published_at datetime(6), shipper_called_at datetime(6), top_up_call_at datetime(6), top_up_closed_at datetime(6), updated_at datetime(6), banner_mode varchar(10), banner_theme varchar(20), code varchar(30) not null, status varchar(30) not null, banner_badge varchar(60), banner_cta_text varchar(60), season varchar(100), banner_headline varchar(200), collection_site_name varchar(200), product_name varchar(200), title varchar(200) not null, banner_subline varchar(300), banner_image_url varchar(500), cancel_reason varchar(500), collection_site_address varchar(500), cover_image_url TEXT, description TEXT, primary key (campaign_id)) engine=InnoDB;
create table cart_items (build_combo_id integer, cart_id integer not null, id integer not null auto_increment, mystery_box_id integer, product_id integer, quantity integer not null, primary key (id)) engine=InnoDB;
create table carts (id integer not null auto_increment, user_id integer not null, primary key (id)) engine=InnoDB;
create table categories (category_id integer not null auto_increment, name varchar(100) not null, description varchar(500), primary key (category_id)) engine=InnoDB;
create table chat_bot (id integer not null auto_increment, user_id integer not null, created_at datetime(6), role varchar(10), content TEXT, primary key (id)) engine=InnoDB;
create table chat_messages (conversation_id integer not null, id integer not null auto_increment, is_read bit, reference_id integer, sender_id integer not null, sent_at datetime(6), attachment_url varchar(255), content TEXT, type varchar(255), primary key (id)) engine=InnoDB;
create table checkout_batches (batch_discount_amount decimal(38,2), buyer_id integer not null, id integer not null auto_increment, total_amount decimal(38,2), created_at datetime(6), updated_at datetime(6), checkout_url TEXT, payment_method varchar(255), payos_order_code varchar(255), qr_code_url TEXT, status enum ('CANCELLED','COMPLETED','PENDING','PROCESSING'), primary key (id)) engine=InnoDB;
create table conversations (id integer not null auto_increment, user1_id integer not null, user2_id integer not null, created_at datetime(6), last_message_at datetime(6), room_key varchar(50) not null, primary key (id)) engine=InnoDB;
create table delivery_hubs (active bit not null, hub_id integer not null auto_increment, latitude float(53) not null, longitude float(53) not null, created_at datetime(6), updated_at datetime(6), phone varchar(20), open_hours varchar(100), name varchar(150) not null, address varchar(500) not null, note varchar(500), primary key (hub_id)) engine=InnoDB;
create table dispatch_offers (distance_km float(53), id integer not null auto_increment, order_id integer not null, shipper_id integer not null, wave integer, created_at datetime(6), primary key (id)) engine=InnoDB;
create table health_conditions (id integer not null auto_increment, name varchar(100) not null, primary key (id)) engine=InnoDB;
create table hub_trips (campaign_id integer not null, confirmed bit not null, driver_id integer, fee_amount decimal(15,2), payout_amount decimal(15,2), pickup_date date, route_km float(53), total_kg decimal(12,2) not null, trip_id integer not null auto_increment, trip_no integer not null, vehicle_kg integer, accepted_at datetime(6), created_at datetime(6), delivered_at datetime(6), paid_at datetime(6), started_at datetime(6), updated_at datetime(6), status varchar(20) not null, load_photo_url varchar(500), declined_driver_ids TEXT, loaded_boxes TEXT, stops_json TEXT, primary key (trip_id)) engine=InnoDB;
create table images (id integer not null auto_increment, return_id integer, review_id integer, with_draw_id integer, created_at datetime(6), image_url varchar(500) not null, media_type enum ('PHOTO','VIDEO'), primary key (id)) engine=InnoDB;
create table invalidated_token (expiry_time datetime(6), id varchar(255) not null, primary key (id)) engine=InnoDB;
create table moderation_labels (confidence decimal(38,2), id integer not null auto_increment, moderation_result_id integer not null, label_name varchar(100), parent_label varchar(100), primary key (id)) engine=InnoDB;
create table moderation_results (id integer not null auto_increment, is_flagged bit, overall_confidence decimal(38,2), product_id integer not null, checked_at datetime(6), flag_reason varchar(500), raw_response LONGTEXT, primary key (id)) engine=InnoDB;
create table mystery_boxes (is_active bit, mystery_id integer not null auto_increment, price decimal(38,2), shop_id integer not null, sold_quantity integer, total_quantity integer, box_type varchar(50), description varchar(500), image_url varchar(500), note varchar(255), primary key (mystery_id)) engine=InnoDB;
create table notifications (admin_id integer, noti_id integer not null auto_increment, reference_id integer, create_at datetime(6), type varchar(40), title varchar(50), receiver_type varchar(100), evidence varchar(500), message TEXT, primary key (noti_id)) engine=InnoDB;
create table nutrition_info (calories decimal(38,2), carbs_g decimal(38,2), fat_g decimal(38,2), id integer not null auto_increment, product_id integer not null, protein_g decimal(38,2), primary key (id)) engine=InnoDB;
create table order_details (combo_id integer, is_requested_return bit, mystery_id integer, order_detail_id integer not null auto_increment, order_id integer not null, product_id integer, quantity integer, unit_price decimal(38,2), item_type varchar(255), primary key (order_detail_id)) engine=InnoDB;
create table orders (batch_discount_amount decimal(38,2), buyer_id integer not null, checkout_batch_id integer, cod_amount decimal(38,2), cod_prepaid_amount decimal(38,2), cod_reserved_amount decimal(38,2), cod_settled bit, dispatch_wave integer, fail_call_attempts integer, order_id integer not null auto_increment, refund_amount decimal(38,2), rescued_weight decimal(38,2), reward_points decimal(38,2), shipper_id integer, shipping_discount_amount decimal(38,2), shipping_fee decimal(38,2), shipping_latitude float(53), shipping_longitude float(53), shop_id integer not null, shop_latitude float(53), shop_longitude float(53), total_amount decimal(38,2), wallet_paid_amount decimal(38,2), cod_cash_confirmed_at datetime(6), cod_refunded_at datetime(6), cod_settled_at datetime(6), completed_at datetime(6), created_at datetime(6), delivered_at datetime(6), dispatch_alerted_at datetime(6), estimated_delivery_date datetime(6), failed_at datetime(6), last_dispatched_at datetime(6), picked_up_at datetime(6), ready_at datetime(6), settled_at datetime(6), shipper_paid_at datetime(6), wallet_refunded_at datetime(6), recipient_phone varchar(20), recipient_name varchar(50), fail_note varchar(500), note varchar(500), delivery_proof_urls TEXT, fail_proof_urls TEXT, payment_method varchar(255), refund_reason varchar(255), shipping_address varchar(255), cod_payment_option enum ('FULL_COD','PREPAID_PARTIAL','PREPAID_SHIPPING'), cod_payment_status enum ('CONFIRMED','PENDING','REFUNDED','SETTLED'), fail_reason enum ('KHAC','KHACH_TU_CHOI','KHONG_LIEN_LAC_DUOC','SAI_DIA_CHI'), status enum ('AWAITING_PAYMENT','CANCELLED','COMPLETED','CONFIRMED','DELIVERED','FAILED','PAID','PENDING','PICKED_UP','READY','REFUNDED','REFUND_REQUESTED','SHIPPING'), primary key (order_id)) engine=InnoDB;
create table otp_verification (id integer not null auto_increment, otp integer not null, created_at datetime(6), expiration_time datetime(6), email varchar(255) not null, primary key (id)) engine=InnoDB;
create table payments (amount decimal(38,2), order_id integer, payment_id integer not null auto_increment, created_at datetime(6), payment_date datetime(6), updated_at datetime(6), checkout_url TEXT, image varchar(255), payment_gateway varchar(255), payos_order_code varchar(255), qr_code_url TEXT, status enum ('CANCELLED','FAILED','PAID','PENDING','REFUNDED'), primary key (payment_id)) engine=InnoDB;
create table plan_days (date date, day_index integer, id integer not null auto_increment, plan_id integer not null, primary key (id)) engine=InnoDB;
create table plan_generation_requests (buyer_id integer not null, id integer not null auto_increment, plan_id integer, requested_at datetime(6), input_params LONGTEXT, raw_ai_response LONGTEXT, primary key (id)) engine=InnoDB;
create table plan_meal_items (combo_id integer, id integer not null auto_increment, is_ai_suggested bit, plan_meal_id integer not null, position integer, quantity integer, unit_price_snapshot decimal(38,2), user_override bit, primary key (id)) engine=InnoDB;
create table plan_meals (completed bit, id integer not null auto_increment, plan_day_id integer not null, sort_order integer, meal_type enum ('BREAKFAST','DINNER','LUNCH','SNACK'), primary key (id)) engine=InnoDB;
create table pre_order_damage_claims (approved_kg decimal(10,2), buyer_id integer not null, claim_id integer not null auto_increment, claimed_kg decimal(10,2) not null, pre_order_id integer not null, refund_amount decimal(15,2), reviewed_by integer, created_at datetime(6), reviewed_at datetime(6), admin_note varchar(500), reason varchar(500), photo_urls varchar(2000) not null, status enum ('APPROVED','PENDING','REJECTED') not null, primary key (claim_id)) engine=InnoDB;
create table pre_orders (buyer_id integer not null, campaign_id integer not null, cod_reserved_amount decimal(15,2), crate_quantity integer not null, deposit_amount decimal(15,2) not null, hub_delivery_pay decimal(15,2), hub_id integer, hub_issue_crates integer, original_crate_quantity integer, pickup_remind_count integer, pre_order_id integer not null auto_increment, remaining_amount decimal(15,2) not null, retry_count integer, shipper_id integer, shipper_income_amount decimal(15,2), shipping_base_fee decimal(38,2), shipping_discount_amount decimal(38,2), shipping_distance_km float(53), shipping_fee decimal(38,2), shipping_latitude float(53), shipping_longitude float(53), shortfall_refund_amount decimal(15,2), source_registration_id integer, total_amount decimal(15,2) not null, total_kg decimal(12,2) not null, unit_price decimal(15,2) not null, cancelled_at datetime(6), created_at datetime(6), delivered_at datetime(6), deposit_paid_at datetime(6), hub_received_at datetime(6), packed_at datetime(6), picked_up_at datetime(6), pickup_reminded_at datetime(6), ready_at datetime(6), updated_at datetime(6), crate_size varchar(10) not null, fulfillment_method varchar(16), recipient_phone varchar(20), status varchar(20) not null, code varchar(30) not null, delivery_zone varchar(100), qr_code varchar(100), recipient_name varchar(100), delivery_ward varchar(150), cancel_reason varchar(500), fail_reason varchar(500), hub_issue_note varchar(500), note varchar(500), shipping_address varchar(500), packed_crates varchar(1000), hub_received_by varchar(255), pod_image_url TEXT, primary key (pre_order_id)) engine=InnoDB;
create table product_allergens (allergen_id integer not null, id integer not null auto_increment, product_id integer not null, primary key (id)) engine=InnoDB;
create table product_combo (combo_id integer not null, product_combo_id integer not null auto_increment, product_id integer not null, product_quantity integer, primary key (product_combo_id)) engine=InnoDB;
create table product_images (display_order integer, is_approved bit default true not null, is_flagged bit default false not null, is_primary bit, product_id integer not null, created_at datetime(6), id bigint not null auto_increment, flag_reason varchar(500), image_url varchar(500) not null, primary key (id)) engine=InnoDB;
create table product_mystery (mystery_id integer not null, product_id integer not null, product_mystery_id integer not null auto_increment, product_quantity integer, primary key (product_mystery_id)) engine=InnoDB;
create table products (category_id integer, discount_percent integer, expiry_date date, product_id integer not null auto_increment, selling_price decimal(38,2), shop_id integer not null, stock_quantity integer, discount_end_date datetime(6), discount_start_date datetime(6), updated_at datetime(6), origin varchar(100), description TEXT, product_name varchar(255) not null, reject_reason TEXT, status varchar(255), unit varchar(255), primary key (product_id)) engine=InnoDB;
create table profile_update_requests (buyer_id integer not null, id integer not null auto_increment, confirmed_at datetime(6), created_at datetime(6), extracted_diff LONGTEXT, raw_input_text TEXT, primary key (id)) engine=InnoDB;
create table referral_attributions (buyer_id integer not null, converted_order_count integer not null, max_conversions integer not null, referrer_id integer not null, target_id integer not null, attributed_at datetime(6) not null, expires_at datetime(6) not null, id bigint not null auto_increment, referral_link_id bigint not null, status enum ('ACTIVE','EXHAUSTED','EXPIRED') not null, target_type enum ('BUILD_COMBO','MYSTERY_BOX','PRODUCT','SHOP') not null, primary key (id)) engine=InnoDB;
create table referral_clicks (buyer_id integer, created_at datetime(6) not null, id bigint not null auto_increment, referral_link_id bigint not null, ip_address varchar(45), user_agent varchar(500), primary key (id)) engine=InnoDB;
create table referral_commissions (base_amount decimal(15,2) not null, buyer_id integer not null, commission_amount decimal(15,2) not null, commission_rate decimal(5,2) not null, order_detail_id integer not null, order_id integer not null, referrer_id integer not null, created_at datetime(6) not null, id bigint not null auto_increment, referral_attribution_id bigint, referral_link_id bigint not null, settled_at datetime(6), updated_at datetime(6), status enum ('CANCELLED','COMPLETED','PENDING') not null, primary key (id)) engine=InnoDB;
create table referral_links (clicks_count integer not null, commission_rate decimal(5,2), is_active bit not null, referrer_id integer not null, target_id integer not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), referral_code varchar(32) not null, target_type enum ('BUILD_COMBO','MYSTERY_BOX','PRODUCT','SHOP') not null, primary key (id)) engine=InnoDB;
create table return_requests (buyer_id integer not null, is_return_goods bit, is_shipping_fee_paid bit, order_detail_id integer not null, refund_amount decimal(38,2), return_id integer not null auto_increment, return_shipping_fee decimal(38,2), shipper_id integer, shop_id integer not null, created_at datetime(6), picked_up_at datetime(6), returned_at datetime(6), updated_at datetime(6), shipping_payment_method varchar(32), admin_remark varchar(1000), shop_response varchar(1000), reason varchar(255), status enum ('ADMIN_APPROVED','APPROVED','CANCELLED','COMPLETED','DISPUTED','PENDING','REJECTED','RETURN_DELIVERED','RETURN_FAILED','RETURN_PICKED_UP','RETURN_SHIPPING','SHOP_APPROVED'), primary key (return_id)) engine=InnoDB;
create table review_reactions (buyer_id integer not null, id integer not null auto_increment, review_id integer not null, created_at datetime(6), updated_at datetime(6), reaction_type enum ('DISLIKE','LIKE'), primary key (id)) engine=InnoDB;
create table reviews (buyer_id integer not null, combo_id integer, mystery_id integer, order_detail_id integer, product_id integer, rating_star decimal(38,2), review_id integer not null auto_increment, shop_id integer, create_at datetime(6), evidence varchar(500), comment TEXT, reply_from_shop TEXT, primary key (review_id)) engine=InnoDB;
create table roles (id integer not null auto_increment, name varchar(100), description varchar(500), primary key (id)) engine=InnoDB;
create table saved_addresses (is_active bit not null, is_default bit not null, latitude float(53), longitude float(53), user_id integer not null, created_at datetime(6), id bigint not null auto_increment, updated_at datetime(6), phone varchar(20) not null, label varchar(50), recipient_name varchar(100) not null, address TEXT not null, primary key (id)) engine=InnoDB;
create table shipper_locations (id integer not null auto_increment, latitude float(53), longitude float(53), online bit, order_id integer, shipper_id integer not null, last_seen_at datetime(6), updated_at datetime(6), delivery_phase varchar(255), primary key (id)) engine=InnoDB;
create table transactions (amount decimal(38,2), balance_after decimal(38,2), payment_id integer not null, transaction_id integer not null auto_increment, content varchar(500), payment_gateway varchar(255), reference_id varchar(255), status enum ('FAILED','PENDING','SUCCESS'), transaction_type enum ('COMMISSION','DEPOSIT','PAYMENT','REFUND','WITHDRAW'), primary key (transaction_id)) engine=InnoDB;
create table user_allergies (allergen_id integer not null, buyer_id integer not null, id integer not null auto_increment, primary key (id)) engine=InnoDB;
create table user_device_tokens (is_active bit not null, user_id integer not null, created_at datetime(6), id bigint not null auto_increment, last_used_at datetime(6), updated_at datetime(6), platform varchar(20), token_type varchar(30), device_name varchar(100), device_token varchar(500) not null, primary key (id)) engine=InnoDB;
create table user_health_conditions (buyer_id integer not null, condition_id integer not null, id integer not null auto_increment, primary key (id)) engine=InnoDB;
create table user_notifications (id integer not null auto_increment, is_deleted bit, is_read bit, notification_id integer not null, user_id integer not null, primary key (id)) engine=InnoDB;
create table users (id integer not null auto_increment, loyalty_points decimal(38,2), rating_average float(53), role_id integer, shop_active bit, shop_latitude float(53), shop_longitude float(53), total_rescued_weight decimal(38,2), create_at datetime(6), locked_at datetime(6), update_at datetime(6), license_class varchar(10), cccd varchar(20), vehicle_type varchar(20), phone_number varchar(50), description varchar(500), achievement varchar(255), address varchar(255), bank_account varchar(255), bank_account_holder varchar(255), bank_name varchar(255), email varchar(255), full_name TEXT, google_id varchar(255), license varchar(255), license_image_url varchar(255), logo_url varchar(255), password varchar(255), shop_name varchar(255), vehicle_doc_image_url varchar(255), vehicle_number varchar(255), diet_preference enum ('NON_VEGETARIAN','VEGAN','VEGETARIAN'), status enum ('ACTIVE','INACTIVE','LOCKED','PENDING'), primary key (id)) engine=InnoDB;
create table voucher_usages (discount_amount decimal(38,2), id integer not null auto_increment, order_id integer not null, user_id integer not null, voucher_id integer not null, used_at datetime(6), primary key (id)) engine=InnoDB;
create table voucher_users (id integer not null auto_increment, quantity integer, quantity_used integer, user_id integer not null, voucher_id integer not null, claim_at datetime(6), primary key (id)) engine=InnoDB;
create table vouchers (active bit, auto_created_from_order_id integer, claim_limit integer, claimed_count integer, discount_value decimal(38,2), max_discount decimal(38,2), min_order_value decimal(38,2), per_user_limit integer, shop_id integer not null, usage_limit integer, used_count integer, voucher_id integer not null auto_increment, create_at datetime(6), expiry_date datetime(6), voucher_code varchar(20) not null, auto_created_reason varchar(100), voucher_scope enum ('PLATFORM','SHOP','SYSTEM','WELCOME'), voucher_target enum ('ITEM','ORDER','SHIPPING','SHOP'), voucher_type enum ('FIXED_AMOUNT','FREE_SHIPPING','PERCENTAGE'), primary key (voucher_id)) engine=InnoDB;
create table wallet_topups (amount decimal(38,2), id integer not null auto_increment, wallet_id integer not null, created_at datetime(6), paid_at datetime(6), payos_order_code varchar(32), checkout_url varchar(500), status enum ('CANCELLED','PAID','PENDING'), primary key (id)) engine=InnoDB;
create table wallet_transactions (amount decimal(15,2) not null, balance_after decimal(15,2) not null, frozen_after decimal(15,2), order_id integer, wallet_id integer not null, created_at datetime(6) not null, id bigint not null auto_increment, referral_commission_id bigint, type varchar(40) not null, description varchar(500), primary key (id)) engine=InnoDB;
create table wallets (cod_reserved decimal(38,2), commission_wallet decimal(38,2), frozen_balance decimal(38,2), total_balance decimal(38,2), total_revenue_all_time decimal(38,2), total_withdrawn decimal(38,2), user_id integer not null, wallet_id integer not null auto_increment, create_at datetime(6), update_at datetime(6), type varchar(255), status enum ('ACTIVE','FROZEN','INACTIVE'), primary key (wallet_id)) engine=InnoDB;
create table withdraw_requests (admin_id integer, amount decimal(38,2), fee decimal(38,2), receive_amount decimal(38,2), request_id integer not null auto_increment, wallet_id integer not null, created_at datetime(6), payout_at datetime(6), processed_at datetime(6), admin_note varchar(255), bank_account_holder varchar(255), bank_account_number varchar(255), bank_name varchar(255), payout_checkout_url TEXT, payout_message varchar(255), payout_order_code varchar(255), payout_provider varchar(255), payout_qr_url TEXT, payout_transaction_id varchar(255), reason varchar(255), payout_status enum ('FAILED','PENDING','PROCESSING','SUCCESS'), status enum ('APPROVED','COMPLETED','FAILED','PENDING','REJECTED'), primary key (request_id)) engine=InnoDB;
alter table blog_likes add constraint UK2r7s5nedkxr29ray23enf1e2 unique (blog_id, user_id);
alter table campaign_deposits add constraint UKm69qh59hxlbk4fy3lw7k8ceci unique (pre_order_id);
alter table campaign_deposits add constraint UKp7rh6knjdvem7iwdtlgmas55e unique (payos_order_code);
create index idx_cfr_campaign_shop on campaign_farm_registrations (campaign_id, shop_id);
create index idx_cpm_shop_status on campaign_proposal_members (shop_id, status);
create index idx_cpm_status_expires on campaign_proposal_members (status, expires_at);
alter table campaign_proposal_members add constraint uk_proposal_shop unique (proposal_id, shop_id);
create index idx_cp_status on campaign_proposals (status, submitted_at);
create index idx_cp_initiator on campaign_proposals (initiator_shop_id, status);
alter table campaign_proposals add constraint UKt5yqwbfjdb758ri3t9exysw82 unique (code);
create index idx_campaign_status_cutoff on campaigns (status, cutoff_at);
alter table campaigns add constraint UKsf34omfajh3s0y1phbhqby50x unique (code);
alter table carts add constraint UK64t7ox312pqal3p7fg9o503c2 unique (user_id);
create index idx_chat_msg_conv_sent on chat_messages (conversation_id, sent_at);
create index idx_chat_msg_conv_sender_read on chat_messages (conversation_id, sender_id, is_read);
create index idx_conv_user1 on conversations (user1_id);
create index idx_conv_user2 on conversations (user2_id);
create index idx_conv_last_msg on conversations (last_message_at);
alter table conversations add constraint UKo3fur2eifewlkt2kaei46ckb7 unique (room_key);
alter table dispatch_offers add constraint uk_dispatch_offer_order_shipper unique (order_id, shipper_id);
create index idx_hub_trip_campaign_status on hub_trips (campaign_id, status);
create index idx_hub_trip_driver on hub_trips (driver_id);
alter table moderation_results add constraint UKl51kly0q0reqvjsvd0yjmhobx unique (product_id);
alter table nutrition_info add constraint UKrjdlave7uqrxfvmp885dho25l unique (product_id);
create index idx_damage_claim_pre_order on pre_order_damage_claims (pre_order_id);
create index idx_damage_claim_status on pre_order_damage_claims (status);
create index idx_pre_order_campaign_status on pre_orders (campaign_id, status);
create index idx_pre_order_buyer on pre_orders (buyer_id);
alter table pre_orders add constraint UK3yngvw5ww8ntwcub88ikkcn11 unique (code);
create index idx_attr_buyer_target on referral_attributions (buyer_id, target_type, target_id, status);
create index idx_attr_expires on referral_attributions (expires_at);
create index idx_click_link_created on referral_clicks (referral_link_id, created_at);
create index idx_ref_comm_order on referral_commissions (order_id);
create index idx_ref_comm_referrer on referral_commissions (referrer_id);
create index idx_ref_comm_status on referral_commissions (status);
alter table referral_commissions add constraint uk_ref_comm_order_detail unique (order_detail_id);
create index idx_ref_code on referral_links (referral_code);
create index idx_ref_referrer on referral_links (referrer_id);
alter table referral_links add constraint uk_ref_code unique (referral_code);
alter table referral_links add constraint uk_referrer_target unique (referrer_id, target_type, target_id);
create index idx_device_token on user_device_tokens (device_token);
create index idx_user_active on user_device_tokens (user_id, is_active);
alter table users add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);
alter table users add constraint UKovh8xmu9ac27t18m56gri58i1 unique (google_id);
alter table voucher_users add constraint uk_voucher_user unique (user_id, voucher_id);
alter table wallet_topups add constraint UKj7r0cfd233lvugh75igx8uau8 unique (payos_order_code);
create index idx_wallet_tx_wallet_created on wallet_transactions (wallet_id, created_at);
create index idx_wallet_tx_order on wallet_transactions (order_id);
create index idx_wallet_tx_ref_comm on wallet_transactions (referral_commission_id);
alter table wallet_transactions add constraint UK9c0ygp3t94hsn1hogj0p6tvi3 unique (referral_commission_id);
alter table blog_comments add constraint FKnadbu7mnnulxvib0jc5dqn7to foreign key (blog_id) references blogs (blog_id);
alter table blog_comments add constraint FKbovv268mg0vg57pkp0nb1bkq4 foreign key (user_id) references users (id);
alter table blog_likes add constraint FKojo8uaqhkmbtqa8duxtbq56w3 foreign key (blog_id) references blogs (blog_id);
alter table blog_likes add constraint FKfqaey79qg6v969ymdp40ft3b foreign key (user_id) references users (id);
alter table blogs add constraint FKg4bdjhd8b47fhqtu5xycfyw9d foreign key (product_id) references products (product_id);
alter table blogs add constraint FKa4kiavf76t09bvoncqurbb4ib foreign key (shop_id) references users (id);
alter table build_combos add constraint FK4h6g63n0o3x3u0c0h985unudm foreign key (shop_id) references users (id);
alter table build_plans add constraint FKoy5h8ot9wjbc6ekp14ab9rm9w foreign key (buyer_id) references users (id);
alter table campaign_deposits add constraint FK4yrddoblcddtdq6egmbw3fqh6 foreign key (pre_order_id) references pre_orders (pre_order_id);
alter table campaign_farm_registrations add constraint FKsgw0rckva66svj0cj65xh7v7o foreign key (campaign_id) references campaigns (campaign_id);
alter table campaign_farm_registrations add constraint FK1obltfqxvh8akx3khokhb9ara foreign key (product_id) references products (product_id);
alter table campaign_farm_registrations add constraint FKnwa4j6wir0oly1772kibw8qi9 foreign key (shop_id) references users (id);
alter table campaign_proposal_members add constraint FK1vuqlpeadeddcxod5ll6th866 foreign key (proposal_id) references campaign_proposals (proposal_id);
alter table campaign_proposal_members add constraint FKa397lhmf09d6bin50p9gxp1e4 foreign key (shop_id) references users (id);
alter table campaign_proposals add constraint FKshl9l0mfuybgo5ca1frvgql1 foreign key (campaign_id) references campaigns (campaign_id);
alter table campaign_proposals add constraint FKnpmtke8ymstj9lpau28j75g3v foreign key (category_id) references categories (category_id);
alter table campaign_proposals add constraint FKif8e5okisd9kormus2n7d9uvv foreign key (initiator_shop_id) references users (id);
alter table campaign_proposals add constraint FKi6skufvc0kgokogf3f1hcq26h foreign key (reviewed_by) references users (id);
alter table campaigns add constraint FKaqf3w59x5i7ckbfwpf8i8hosu foreign key (category_id) references categories (category_id);
alter table campaigns add constraint FKyfby4s4hyhrlmj6j3c63xt6 foreign key (created_by) references users (id);
alter table cart_items add constraint FKgr0gydhnii2pkl82u9pu4e4u5 foreign key (build_combo_id) references build_combos (combo_id);
alter table cart_items add constraint FKpcttvuq4mxppo8sxggjtn5i2c foreign key (cart_id) references carts (id);
alter table cart_items add constraint FKq9xjsnpoti9ig6job0l6s2qil foreign key (mystery_box_id) references mystery_boxes (mystery_id);
alter table cart_items add constraint FK1re40cjegsfvw58xrkdp6bac6 foreign key (product_id) references products (product_id);
alter table carts add constraint FKb5o626f86h46m4s7ms6ginnop foreign key (user_id) references users (id);
alter table chat_bot add constraint FK8fgiuyajesop5rhj1utcp2c5q foreign key (user_id) references users (id);
alter table chat_messages add constraint FKc8ljv426x8fj9tcywei40stu9 foreign key (conversation_id) references conversations (id);
alter table chat_messages add constraint FKgiqeap8ays4lf684x7m0r2729 foreign key (sender_id) references users (id);
alter table checkout_batches add constraint FK5kn4nxiwkc6ko8smj7n6690bt foreign key (buyer_id) references users (id);
alter table conversations add constraint FK8wv0rmd8jb3cqcbyng15ubrmk foreign key (user1_id) references users (id);
alter table conversations add constraint FKe7w0k1xem21pp85wxh5moodnk foreign key (user2_id) references users (id);
alter table dispatch_offers add constraint FKqxf4lfjenq74i4uo6gftrerus foreign key (order_id) references orders (order_id);
alter table dispatch_offers add constraint FKo48bpdm3vt7kksfjsmbccob00 foreign key (shipper_id) references users (id);
alter table hub_trips add constraint FK6gg0p7b4omig0bkp484ji3la2 foreign key (campaign_id) references campaigns (campaign_id);
alter table hub_trips add constraint FK6aeagskdtqtgre97399dfhag7 foreign key (driver_id) references users (id);
alter table images add constraint FKgxm12j9th736iupo782lucaii foreign key (return_id) references return_requests (return_id);
alter table images add constraint FKldf2s8wlcsqgmmrokc6syxw5q foreign key (review_id) references reviews (review_id);
alter table images add constraint FK8oua7wx59on68pion1dcsdmsk foreign key (with_draw_id) references withdraw_requests (request_id);
alter table moderation_labels add constraint FK1hei7o3wgnpr916cp7gkt31py foreign key (moderation_result_id) references moderation_results (id);
alter table moderation_results add constraint FKjmf1w1eyap1u9g2d7bb39d218 foreign key (product_id) references products (product_id);
alter table mystery_boxes add constraint FKrioanlids15inu2u166wfrp4i foreign key (shop_id) references users (id);
alter table notifications add constraint FK3n39hhdax4bsti9y94vvqq573 foreign key (admin_id) references users (id);
alter table nutrition_info add constraint FKfw0jfroy28gsi5us0gp7xo3p3 foreign key (product_id) references products (product_id);
alter table order_details add constraint FKgs5ahlb018w90hm54v6hcmcsn foreign key (combo_id) references build_combos (combo_id);
alter table order_details add constraint FKcdrhs2rhqdfreuescrbaul6ui foreign key (mystery_id) references mystery_boxes (mystery_id);
alter table order_details add constraint FKjyu2qbqt8gnvno9oe9j2s2ldk foreign key (order_id) references orders (order_id);
alter table order_details add constraint FK4q98utpd73imf4yhttm3w0eax foreign key (product_id) references products (product_id);
alter table orders add constraint FKhtx3insd5ge6w486omk4fnk54 foreign key (buyer_id) references users (id);
alter table orders add constraint FKdvmywedcw8l93sbfsj1nyuuya foreign key (checkout_batch_id) references checkout_batches (id);
alter table orders add constraint FKmjnf91amspoh2m4qg8ct5y8uj foreign key (shipper_id) references users (id);
alter table orders add constraint FK52l2y22apljc0a0v1grlq8obv foreign key (shop_id) references users (id);
alter table payments add constraint FK81gagumt0r8y3rmudcgpbk42l foreign key (order_id) references orders (order_id);
alter table plan_days add constraint FKb9buwrnqfed1ilagerwga58f9 foreign key (plan_id) references build_plans (plan_id);
alter table plan_generation_requests add constraint FK4y61bfo6gnrnkeqey431yu9u3 foreign key (plan_id) references build_plans (plan_id);
alter table plan_generation_requests add constraint FKqowbxrlwqgk29i6m4w281t560 foreign key (buyer_id) references users (id);
alter table plan_meal_items add constraint FKhb7cm5s4qlniiygmxsi4b9r9 foreign key (combo_id) references build_combos (combo_id);
alter table plan_meal_items add constraint FKaqu3pyhsmt49mp2mw13s679tq foreign key (plan_meal_id) references plan_meals (id);
alter table plan_meals add constraint FK314xbj22woc6w48gyhtmmkmj8 foreign key (plan_day_id) references plan_days (id);
alter table pre_order_damage_claims add constraint FKa0mf1hecgrugrtvaf8m9kjwwa foreign key (buyer_id) references users (id);
alter table pre_order_damage_claims add constraint FK7a95wd3levdj6rntnkp98nsjo foreign key (pre_order_id) references pre_orders (pre_order_id);
alter table pre_order_damage_claims add constraint FKnlhqidmoamdfo5m0wq39k231r foreign key (reviewed_by) references users (id);
alter table pre_orders add constraint FKsocydu98fsw0ffj13g28ttla2 foreign key (buyer_id) references users (id);
alter table pre_orders add constraint FKe79m5pl9he7kmoqevopblhty7 foreign key (campaign_id) references campaigns (campaign_id);
alter table pre_orders add constraint FKt14tc4mascmofio4falc3mqdo foreign key (hub_id) references delivery_hubs (hub_id);
alter table pre_orders add constraint FK6j9brrs1dxl41mdnl8cml5g3d foreign key (shipper_id) references users (id);
alter table pre_orders add constraint FKfy9c0m7i49ushpkix1soo3swh foreign key (source_registration_id) references campaign_farm_registrations (registration_id);
alter table product_allergens add constraint FK95v23b3wm2nnbjks4up3gwvkg foreign key (allergen_id) references allergens (id);
alter table product_allergens add constraint FKps3ek2a1xvrk0h8dqaqn29hb1 foreign key (product_id) references products (product_id);
alter table product_combo add constraint FK5xvho7n3rh7crh9opmqa1m4iw foreign key (combo_id) references build_combos (combo_id);
alter table product_combo add constraint FK1c0ym4ftbabj94de77ye3kpu7 foreign key (product_id) references products (product_id);
alter table product_images add constraint FKqnq71xsohugpqwf3c9gxmsuy foreign key (product_id) references products (product_id);
alter table product_mystery add constraint FKm869lltgm0ox1hbbrx3n4l65h foreign key (mystery_id) references mystery_boxes (mystery_id);
alter table product_mystery add constraint FKg65228ygkkn88ox7326xcm51g foreign key (product_id) references products (product_id);
alter table products add constraint FKog2rp4qthbtt2lfyhfo32lsw9 foreign key (category_id) references categories (category_id);
alter table products add constraint FK2nwen8vfrq31slojk8bxj97pq foreign key (shop_id) references users (id);
alter table profile_update_requests add constraint FK5ts6ytas2jnufj04sf1y16npv foreign key (buyer_id) references users (id);
alter table referral_attributions add constraint FK2meilp9ejve7qh6l9pqrdll3t foreign key (buyer_id) references users (id);
alter table referral_attributions add constraint FKpwdt9563o7nod2ugacvw82rdf foreign key (referral_link_id) references referral_links (id);
alter table referral_attributions add constraint FKbs4po317i1efe0csok77mshlo foreign key (referrer_id) references users (id);
alter table referral_clicks add constraint FKp046qahled9hhrbq9kq49pnda foreign key (buyer_id) references users (id);
alter table referral_clicks add constraint FKh3vjtj0hclxc3rrek5ah8x9f3 foreign key (referral_link_id) references referral_links (id);
alter table referral_commissions add constraint FKqqrvpt8dne25j7jsuee00e7vg foreign key (buyer_id) references users (id);
alter table referral_commissions add constraint FKlcgac674wj1kylhogfn1aaukb foreign key (order_id) references orders (order_id);
alter table referral_commissions add constraint FK4fqhqr61dtx6230bvey3tqn12 foreign key (order_detail_id) references order_details (order_detail_id);
alter table referral_commissions add constraint FKpy2fgr3828p1eirnx4ey2w5w0 foreign key (referral_attribution_id) references referral_attributions (id);
alter table referral_commissions add constraint FKj6mru4xsgp4eyq65pta64l2dt foreign key (referral_link_id) references referral_links (id);
alter table referral_commissions add constraint FKmkiciqltp1k1hyj0om2i9ac3d foreign key (referrer_id) references users (id);
alter table referral_links add constraint FKs79dvti0g7s48byk9jas1li8 foreign key (referrer_id) references users (id);
alter table return_requests add constraint FK2x5c7kwyx72wwpm68d19xkoey foreign key (buyer_id) references users (id);
alter table return_requests add constraint FKchd0thli7nmuccsg3kjj6ec4x foreign key (order_detail_id) references order_details (order_detail_id);
alter table return_requests add constraint FKdi5uba913al35ejdrbdj016rk foreign key (shipper_id) references users (id);
alter table return_requests add constraint FKfjrhk8sai6kg0kbk2o13jwxt2 foreign key (shop_id) references users (id);
alter table review_reactions add constraint FKagr32pdbkumqgp815prhhfo9d foreign key (buyer_id) references users (id);
alter table review_reactions add constraint FKji3edp610yvsra9yyt29rgm9u foreign key (review_id) references reviews (review_id);
alter table reviews add constraint FKd56v67w657bxxni0jae1y0fiu foreign key (combo_id) references build_combos (combo_id);
alter table reviews add constraint FK2noibxu5e960l1c3wk929342s foreign key (buyer_id) references users (id);
alter table reviews add constraint FKst85t8aalirodrny444ny6jsa foreign key (mystery_id) references mystery_boxes (mystery_id);
alter table reviews add constraint FKa6fy7npl2qi9nu2fo9igepm9k foreign key (order_detail_id) references order_details (order_detail_id);
alter table reviews add constraint FKpl51cejpw4gy5swfar8br9ngi foreign key (product_id) references products (product_id);
alter table reviews add constraint FKf7htnh5cn6mc1iap59afgwjl5 foreign key (shop_id) references users (id);
alter table saved_addresses add constraint FKu4i2opv686b8g0c943yqvm46 foreign key (user_id) references users (id);
alter table shipper_locations add constraint FKl493mn7xvjnpfo8lrtgiqsjan foreign key (order_id) references orders (order_id);
alter table shipper_locations add constraint FKai8s7ee3y9lcoblsvoi42d3ku foreign key (shipper_id) references users (id);
alter table transactions add constraint FKmt44qv8av8abvaqb5nbhjnmi2 foreign key (payment_id) references payments (payment_id);
alter table user_allergies add constraint FK1enk7uwa8v1og9dil92khav7i foreign key (allergen_id) references allergens (id);
alter table user_allergies add constraint FKee9oa7hwfervje4ggta9uok0g foreign key (buyer_id) references users (id);
alter table user_device_tokens add constraint FKevk16cemgy1bwx5s0d6oa8x41 foreign key (user_id) references users (id);
alter table user_health_conditions add constraint FKiqy57cw9p2fnpcrqn1qy99ti5 foreign key (buyer_id) references users (id);
alter table user_health_conditions add constraint FKap2884qlcl1cgtf5cbwoiawfv foreign key (condition_id) references health_conditions (id);
alter table user_notifications add constraint FKovvx0ab3h8s9lrm6fppuadn7d foreign key (notification_id) references notifications (noti_id);
alter table user_notifications add constraint FK9f86wonnl11hos1cuf5fibutl foreign key (user_id) references users (id);
alter table users add constraint FKp56c1712k691lhsyewcssf40f foreign key (role_id) references roles (id);
alter table voucher_usages add constraint FKc6ctsyf63q19vkjb6k3lrecev foreign key (order_id) references orders (order_id);
alter table voucher_usages add constraint FKayocv8v53p94c3jr2ookjd4jq foreign key (user_id) references users (id);
alter table voucher_usages add constraint FKguvf95urdn2namu0hgiasgttx foreign key (voucher_id) references vouchers (voucher_id);
alter table voucher_users add constraint FKgcq0hnw08m7r3kkg82ggxi40i foreign key (user_id) references users (id);
alter table voucher_users add constraint FKhch97p4o3m93b69juyxcm58j6 foreign key (voucher_id) references vouchers (voucher_id);
alter table vouchers add constraint FK5i8v6m9wrwtu0ig59551qdl3i foreign key (shop_id) references users (id);
alter table wallet_topups add constraint FKgcabh1c5wnt2ot4j1jgfuvgh9 foreign key (wallet_id) references wallets (wallet_id);
alter table wallet_transactions add constraint FKetfi6nehbld4k4ma3oshxpp8j foreign key (order_id) references orders (order_id);
alter table wallet_transactions add constraint FK8seu7b87ifqi09ghhssusmb0x foreign key (wallet_id) references wallets (wallet_id);
alter table wallets add constraint FKc1foyisidw7wqqrkamafuwn4e foreign key (user_id) references users (id);
alter table withdraw_requests add constraint FKofvg6jjm70i73jfffj4ivhaf foreign key (admin_id) references users (id);
alter table withdraw_requests add constraint FK5vx22tfga0q5qi2k0fmh3tt4o foreign key (wallet_id) references wallets (wallet_id);

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------------------------
-- B. DỮ LIỆU TEST
-- ------------------------------------------------------------------------------
SET @now   := NOW();
SET @today := CURDATE();
SET @pwd   := '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K';
SET @logo  := 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252594/brands/srflhqfol1ckwavsaa1n.jpg';
SET @old_safe := @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- ------------------------------------------------------------------------------
-- 1. VAI TRÒ, DANH MỤC, ADMIN
-- ------------------------------------------------------------------------------
INSERT INTO roles (id, name, description) VALUES
(1, 'BUYER',   'Role dành cho Người mua hàng'),
(2, 'SHOP',    'Role dành cho Cửa hàng / Nhà vườn CapNong'),
(3, 'SHIPPER', 'Role dành cho Người giao hàng'),
(4, 'ADMIN',   'Role dành cho Quản trị viên hệ thống');

INSERT INTO categories (category_id, name, description) VALUES
(1, 'Rau lá', 'Các loại rau xanh tươi sạch: rau muống, cải, xà lách, rau thơm thu hoạch sáng sớm.'),
(2, 'Củ', 'Các loại củ tươi ngon: cà rốt, khoai tây, khoai lang, củ cải, hành tỏi Đà Lạt.'),
(3, 'Quả / Trái cây', 'Trái cây tươi đặc sản: chuối, bưởi da xanh, xoài cát, cam sành, dâu tây mọng nước.'),
(4, 'Nông sản hữu cơ VietGAP', 'Nông sản canh tác tự nhiên đạt chuẩn chứng nhận an toàn thực phẩm.'),
(5, 'Combo & Túi mù', 'Gói rau củ theo món ăn và túi mù nông sản tiết kiệm bảo vệ môi trường.');

INSERT INTO users (email, password, role_id, status, full_name, phone_number, address, shop_active, bank_name, bank_account,
                   bank_account_holder, rating_average, description, create_at)
VALUES ('admin@gmail.com', @pwd, 4, 'ACTIVE', 'Quản Trị Viên Hệ Thống', '0900000000', '1 Công Xã Paris, Quận 1, TP. Hồ Chí Minh', 0,
        'Vietcombank', '0011001234567', 'CAPNONG ADMIN', 5.0, 'Quản trị viên toàn hệ thống CapNong', @now);
SET @admin := LAST_INSERT_ID();

-- ------------------------------------------------------------------------------
-- 2. SHOP / NHÀ VƯỜN (toạ độ thật để tìm shop lân cận)
-- ------------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_hv_shop;
CREATE TEMPORARY TABLE tmp_hv_shop (
    n INT, owner VARCHAR(100), shop VARCHAR(150), address VARCHAR(255), lat DOUBLE, lon DOUBLE, active TINYINT
) DEFAULT CHARSET = utf8mb4;
INSERT INTO tmp_hv_shop VALUES
-- Cụm Hóc Môn - Củ Chi - Bình Chánh
( 1,'Trần Văn Kiệt', 'Vườn Rau Xuân Thới Sơn',     'Xã Xuân Thới Sơn, Huyện Hóc Môn, TP. Hồ Chí Minh',     10.8800, 106.5700, 1),
( 2,'Lê Thị Hoa',    'Nông Trại Tân Xuân',         'Xã Tân Xuân, Huyện Hóc Môn, TP. Hồ Chí Minh',           10.8855, 106.6010, 1),
( 3,'Phạm Minh Tâm', 'Vườn Củ Quả Bà Điểm',        'Xã Bà Điểm, Huyện Hóc Môn, TP. Hồ Chí Minh',            10.8420, 106.5960, 1),
( 4,'Võ Thị Lan',    'Hợp Tác Xã Nhị Bình',        'Xã Nhị Bình, Huyện Hóc Môn, TP. Hồ Chí Minh',           10.9010, 106.6710, 1),
( 5,'Huỳnh Văn Phúc','Vườn Bí Củ Chi',             'Xã Tân Phú Trung, Huyện Củ Chi, TP. Hồ Chí Minh',       10.9560, 106.5560, 1),
( 6,'Đỗ Thị Mai',    'Vườn Nhà Mai Vĩnh Lộc',      'Xã Vĩnh Lộc A, Huyện Bình Chánh, TP. Hồ Chí Minh',      10.8230, 106.5780, 1),
-- Cụm Thủ Đức
( 7,'Nguyễn Thị Thu','Vườn Rau Sạch Linh Trung',   '12 Đường số 9, Phường Linh Trung, TP. Thủ Đức, TP. Hồ Chí Minh',   10.8700, 106.8030, 1),
( 8,'Trần Văn Lộc',  'Nông Trại Hiệp Phú',         '45 Lê Văn Việt, Phường Hiệp Phú, TP. Thủ Đức, TP. Hồ Chí Minh',    10.8470, 106.7770, 1),
( 9,'Lý Thị Hạnh',   'Vườn Trái Cây Bình Thọ',     '8 Đặng Văn Bi, Phường Bình Thọ, TP. Thủ Đức, TP. Hồ Chí Minh',     10.8460, 106.7650, 1),
(10,'Bùi Văn Sơn',   'Nông Sản Long Trường',       '120 Nguyễn Duy Trinh, Phường Long Trường, TP. Thủ Đức, TP. Hồ Chí Minh', 10.8080, 106.8170, 1),
(11,'Ngô Thị Yến',   'Vườn Tam Bình',              '30 Gò Dưa, Phường Tam Bình, TP. Thủ Đức, TP. Hồ Chí Minh',         10.8720, 106.7400, 1),
(12,'Lý Văn Nam',    'Vườn Rau Tạm Ngưng',         '30 Lê Văn Việt, Phường Hiệp Phú, TP. Thủ Đức, TP. Hồ Chí Minh',    10.8490, 106.7740, 0);

INSERT INTO users (email, password, role_id, status, full_name, phone_number, address, shop_name, shop_active, bank_name, bank_account,
                   bank_account_holder, rating_average, description, logo_url, shop_latitude, shop_longitude, create_at)
SELECT CONCAT('shop', t.n, '@gmail.com'), @pwd, 2, 'ACTIVE', t.owner, CONCAT('0933', LPAD(t.n, 6, '0')), t.address, t.shop, t.active,
       'Vietcombank', CONCAT('0071', LPAD(t.n, 9, '0')), UPPER(CONCAT('NHA VUON ', t.n)), 4.8,
       'Nhà vườn TP. Hồ Chí Minh dùng để test chiến dịch giải cứu.', @logo, t.lat, t.lon, @now
FROM tmp_hv_shop t ORDER BY t.n;

-- Mỗi shop bán vài mặt hàng ở các danh mục khác nhau (để lọc "cùng danh mục" khi mời shop)
DROP TEMPORARY TABLE IF EXISTS tmp_hv_product;
CREATE TEMPORARY TABLE tmp_hv_product (
    shop INT, category INT, name VARCHAR(150), price INT, stock INT
) DEFAULT CHARSET = utf8mb4;
INSERT INTO tmp_hv_product VALUES
(1,2,'Khoai lang mật',22000,400),(1,1,'Cải ngọt',18000,150),
(2,2,'Khoai lang tím',24000,300),(2,3,'Bí đỏ hồ lô',16000,250),
(3,2,'Củ cải trắng',14000,200),(3,3,'Dưa leo',17000,180),
(4,1,'Rau muống',12000,160),(4,1,'Cải thìa',16000,120),
(5,2,'Khoai mì',11000,500),(5,3,'Bí đao',13000,300),
(6,1,'Xà lách',25000,90),(6,3,'Cà chua',20000,200),
(7,1,'Rau dền',14000,100),(7,2,'Cà rốt',21000,150),
(8,2,'Khoai tây',23000,220),(8,3,'Khổ qua',19000,130),
(9,3,'Xoài cát',35000,200),(9,3,'Ổi nữ hoàng',22000,180),
(10,1,'Rau thơm',30000,60),(10,3,'Chanh không hạt',18000,140),
(11,2,'Khoai lang mật',22000,180),(11,1,'Cải bẹ xanh',17000,110),
(12,1,'Rau muống',12000,50);

INSERT INTO products (shop_id, category_id, product_name, unit, origin, selling_price, stock_quantity, description, status, updated_at, discount_percent)
SELECT u.id, t.category, t.name, 'kg', 'TP. Hồ Chí Minh', t.price, t.stock,
       CONCAT(t.name, ' thu hoạch trong ngày tại vườn.'), 'ACTIVE', @now, 0
FROM tmp_hv_product t
JOIN users u ON u.email = CONCAT('shop', t.shop, '@gmail.com') COLLATE utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 3. HUB KHẮP TP. HỒ CHÍ MINH
-- ------------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_hv_hub;
CREATE TEMPORARY TABLE tmp_hv_hub (
    n INT, name VARCHAR(150), address VARCHAR(500), lat DOUBLE, lon DOUBLE
) DEFAULT CHARSET = utf8mb4;
INSERT INTO tmp_hv_hub VALUES
( 1,'Hub Quận 1',      '65 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh',              10.7745, 106.7035),
( 2,'Hub Quận 3',      '86 Võ Văn Tần, Phường Võ Thị Sáu, Quận 3, TP. Hồ Chí Minh',             10.7770, 106.6890),
( 3,'Hub Quận 4',      '130 Hoàng Diệu, Phường 12, Quận 4, TP. Hồ Chí Minh',                    10.7630, 106.7025),
( 4,'Hub Quận 5',      '500 Trần Hưng Đạo, Phường 7, Quận 5, TP. Hồ Chí Minh',                  10.7540, 106.6700),
( 5,'Hub Quận 6',      '230 Hậu Giang, Phường 9, Quận 6, TP. Hồ Chí Minh',                      10.7500, 106.6350),
( 6,'Hub Quận 7',      '15 Nguyễn Thị Thập, Phường Tân Phong, Quận 7, TP. Hồ Chí Minh',         10.7380, 106.7150),
( 7,'Hub Quận 8',      '800 Phạm Thế Hiển, Phường 4, Quận 8, TP. Hồ Chí Minh',                  10.7400, 106.6620),
( 8,'Hub Quận 10',     '50 Thành Thái, Phường 12, Quận 10, TP. Hồ Chí Minh',                    10.7730, 106.6650),
( 9,'Hub Quận 11',     '300 Lạc Long Quân, Phường 5, Quận 11, TP. Hồ Chí Minh',                 10.7650, 106.6410),
(10,'Hub Quận 12',     '1 Nguyễn Văn Quá, Phường Đông Hưng Thuận, Quận 12, TP. Hồ Chí Minh',    10.8420, 106.6270),
(11,'Hub Bình Thạnh',  '220 Xô Viết Nghệ Tĩnh, Phường 21, Quận Bình Thạnh, TP. Hồ Chí Minh',    10.8030, 106.7100),
(12,'Hub Gò Vấp',      '200 Quang Trung, Phường 10, Quận Gò Vấp, TP. Hồ Chí Minh',              10.8350, 106.6700),
(13,'Hub Phú Nhuận',   '150 Phan Xích Long, Phường 2, Quận Phú Nhuận, TP. Hồ Chí Minh',         10.7990, 106.6830),
(14,'Hub Tân Bình',    '364 Cộng Hòa, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh',               10.8010, 106.6440),
(15,'Hub Tân Phú',     '150 Tân Kỳ Tân Quý, Phường Sơn Kỳ, Quận Tân Phú, TP. Hồ Chí Minh',      10.8000, 106.6190),
(16,'Hub Bình Tân',    '100 Kinh Dương Vương, Phường An Lạc, Quận Bình Tân, TP. Hồ Chí Minh',   10.7440, 106.6180),
(17,'Hub Thủ Đức',     '20 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh',       10.8506, 106.7719),
(18,'Hub An Phú',      '100 Mai Chí Thọ, Phường An Phú, TP. Thủ Đức, TP. Hồ Chí Minh',          10.7925, 106.7480),
(19,'Hub Phước Long',  '50 Đỗ Xuân Hợp, Phường Phước Long B, TP. Thủ Đức, TP. Hồ Chí Minh',     10.8200, 106.7730),
(20,'Hub Hóc Môn',     '2 Lý Thường Kiệt, Thị trấn Hóc Môn, Huyện Hóc Môn, TP. Hồ Chí Minh',    10.8865, 106.5945),
(21,'Hub Củ Chi',      '150 Tỉnh lộ 8, Thị trấn Củ Chi, Huyện Củ Chi, TP. Hồ Chí Minh',         10.9730, 106.4930),
(22,'Hub Bình Chánh',  'Đường Nguyễn Hữu Trí, Thị trấn Tân Túc, Huyện Bình Chánh, TP. Hồ Chí Minh', 10.6880, 106.5740),
(23,'Hub Nhà Bè',      '1300 Huỳnh Tấn Phát, Thị trấn Nhà Bè, Huyện Nhà Bè, TP. Hồ Chí Minh',   10.6950, 106.7390),
(24,'Hub Cần Giờ',     'Đường Duyên Hải, Thị trấn Cần Thạnh, Huyện Cần Giờ, TP. Hồ Chí Minh',   10.4110, 106.9540);

INSERT INTO delivery_hubs (name, address, latitude, longitude, phone, open_hours, note, active, created_at, updated_at)
SELECT t.name, t.address, t.lat, t.lon, CONCAT('0933100', LPAD(t.n, 3, '0')), '6:00 – 20:00', NULL, 1, @now, @now
FROM tmp_hv_hub t ORDER BY t.n;

-- ------------------------------------------------------------------------------
-- 4. NGƯỜI MUA (kèm sổ địa chỉ và ví để cọc)
-- ------------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_hv_buyer;
CREATE TEMPORARY TABLE tmp_hv_buyer (
    n INT, full_name VARCHAR(100), address VARCHAR(255), lat DOUBLE, lon DOUBLE
) DEFAULT CHARSET = utf8mb4;
INSERT INTO tmp_hv_buyer VALUES
( 1,'Ngô Gia Sang',          '45 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh',              10.7730, 106.7010),
( 2,'Nguyễn Thị Minh Châu',  '120 Võ Văn Tần, Phường Võ Thị Sáu, Quận 3, TP. Hồ Chí Minh',       10.7760, 106.6880),
( 3,'Trần Quốc Bảo',         '101 Nguyễn Thị Thập, Phường Tân Phú, Quận 7, TP. Hồ Chí Minh',      10.7390, 106.7100),
( 4,'Lê Hoàng Yến',          '12 Đường số 7, Phường Linh Trung, TP. Thủ Đức, TP. Hồ Chí Minh',    10.8620, 106.7760),
( 5,'Đỗ Minh Khoa',          '55 Quang Trung, Phường 10, Quận Gò Vấp, TP. Hồ Chí Minh',           10.8330, 106.6660),
( 6,'Bùi Thu Trang',         '9 Hoàng Hoa Thám, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh',       10.8030, 106.6480),
( 7,'Hoàng Gia Huy',         '208 Xô Viết Nghệ Tĩnh, Phường 21, Quận Bình Thạnh, TP. Hồ Chí Minh',10.8040, 106.7110),
( 8,'Trịnh Ngọc Ánh',        '20 Tô Ký, Phường Trung Mỹ Tây, Quận 12, TP. Hồ Chí Minh',           10.8520, 106.6200),
( 9,'Lý Thanh Tùng',         '45 Tên Lửa, Phường An Lạc A, Quận Bình Tân, TP. Hồ Chí Minh',       10.7500, 106.6140),
(10,'Đặng Mỹ Linh',          '285 Cách Mạng Tháng Tám, Phường 12, Quận 10, TP. Hồ Chí Minh',      10.7790, 106.6750);

INSERT INTO users (email, password, role_id, status, full_name, phone_number, address, shop_active, rating_average, description, create_at)
SELECT CONCAT('buyer', t.n, '@gmail.com'), @pwd, 1, 'ACTIVE', t.full_name, CONCAT('0911', LPAD(t.n, 6, '0')), t.address, 0, 5.0,
       'Người mua dùng để test chiến dịch', @now
FROM tmp_hv_buyer t ORDER BY t.n;

INSERT INTO saved_addresses (user_id, label, recipient_name, phone, address, latitude, longitude, is_default, is_active, created_at, updated_at)
SELECT u.id, 'Nhà', u.full_name, u.phone_number, t.address, t.lat, t.lon, 1, 1, @now, @now
FROM tmp_hv_buyer t
JOIN users u ON u.email = CONCAT('buyer', t.n, '@gmail.com') COLLATE utf8mb4_unicode_ci;

INSERT INTO wallets (user_id, status, type, total_balance, frozen_balance, total_revenue_all_time, total_withdrawn, commission_wallet, cod_reserved, create_at, update_at)
SELECT u.id, 'ACTIVE', 'BUYER', 20000000, 0, 0, 0, 0, 0, @now, @now
FROM users u WHERE u.role_id = 1;

-- ------------------------------------------------------------------------------
-- 5. TÀI XẾ XE RA HUB VÀ SHIPPER GIAO TẬN NHÀ
-- ------------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_hv_ship;
CREATE TEMPORARY TABLE tmp_hv_ship (
    email VARCHAR(100), full_name VARCHAR(100), phone VARCHAR(20), plate VARCHAR(20), vtype VARCHAR(20), lclass VARCHAR(10), lat DOUBLE, lon DOUBLE
) DEFAULT CHARSET = utf8mb4;
INSERT INTO tmp_hv_ship VALUES
-- Xe ra hub, khoảng cách tính từ điểm tập kết Hóc Môn
('taixe1@gmail.com',    'Nguyễn Văn Tải (1 tấn, 1 km)',    '0944000001', '51C-100.01', 'TRUCK_1T',  'C',  10.8930, 106.5990),
('taixe2@gmail.com',    'Võ Thành Gom (Bán tải, 2 km)',    '0944000002', '51D-050.02', 'VAN',       'B2', 10.8760, 106.5860),
('taixe3@gmail.com',    'Trần Minh Chở (1 tấn, 2,5 km)',   '0944000003', '51C-100.03', 'TRUCK_1T',  'B2', 10.8700, 106.6050),
('taixe4@gmail.com',    'Lê Quốc Vận (2 tấn, 4 km)',       '0944000004', '51C-200.04', 'TRUCK_2T',  'C',  10.8530, 106.6030),
('taixe5@gmail.com',    'Đỗ Hữu Đức (2 tấn, 8 km)',        '0944000005', '51C-200.05', 'TRUCK_2T',  'C',  10.8300, 106.6300),
('taixe6@gmail.com',    'Phạm Đức Hàng (5 tấn, 9 km)',     '0944000006', '51C-500.06', 'TRUCK_5T',  'C',  10.8200, 106.5500),
('taixe7@gmail.com',    'Hồ Văn Mạnh (5 tấn, 14 km)',      '0944000007', '51C-500.07', 'TRUCK_5T',  'CE', 10.7700, 106.6300),
('taixe8@gmail.com',    'Ngô Tấn Lộc (1 tấn, 16 km)',      '0944000008', '51C-100.08', 'TRUCK_1T',  'C',  10.8000, 106.7100),
('taixe9@gmail.com',    'Lâm Quốc Thái (5 tấn, 25 km)',    '0944000009', '51C-500.09', 'TRUCK_5T',  'C',  10.8500, 106.8200),
-- Shipper giao tận nhà
('giaohang1@gmail.com', 'Bùi Anh Giao (Quận 7)',           '0944000011', '59D-110.11', 'VAN',       'B2', 10.7385, 106.7155),
('giaohang2@gmail.com', 'Hồ Nhật Phát (Thủ Đức)',          '0944000012', '59D-120.12', 'VAN',       'B2', 10.8510, 106.7725),
('giaohang3@gmail.com', 'Mai Thanh Nhanh (Tân Bình)',      '0944000013', '59D-130.13', 'VAN',       'B2', 10.8012, 106.6445),
('giaohang4@gmail.com', 'Cao Văn Lẹ (Xe máy, Gò Vấp)',     '0944000014', '59-X1 140.14', 'MOTORBIKE','A1', 10.8352, 106.6705);

INSERT INTO users (email, password, role_id, status, full_name, phone_number, address, shop_active, bank_name, bank_account,
                   bank_account_holder, rating_average, description, license, license_class, vehicle_number, vehicle_type, create_at)
SELECT t.email, @pwd, 3, 'ACTIVE', t.full_name, t.phone, 'TP. Hồ Chí Minh', 0, 'VietinBank', CONCAT('1019', t.phone),
       'TAI XE TEST', 4.8, 'Tài xế dùng để test chiến dịch', CONCAT('79', t.phone), t.lclass, t.plate, t.vtype, @now
FROM tmp_hv_ship t;

INSERT INTO wallets (user_id, status, type, total_balance, frozen_balance, total_revenue_all_time, total_withdrawn, commission_wallet, cod_reserved, create_at, update_at)
SELECT u.id, 'ACTIVE', 'SHIPPER', 30000000, 0, 0, 0, 0, 0, @now, @now
FROM users u JOIN tmp_hv_ship t ON u.email = t.email COLLATE utf8mb4_unicode_ci;

INSERT INTO shipper_locations (order_id, shipper_id, latitude, longitude, updated_at, delivery_phase, online, last_seen_at)
SELECT NULL, u.id, t.lat, t.lon, @now, NULL, 1, @now
FROM users u JOIN tmp_hv_ship t ON u.email = t.email COLLATE utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 6. CHIẾN DỊCH ĐÃ TỚI NGÀY MỞ ĐIỂM TẬP KẾT (thu hoạch xong, hôm nay gom hàng)
-- ------------------------------------------------------------------------------
SET @site_name := 'Điểm tập kết Hóc Môn - HTX Nông Sản Xuân Thới';
SET @site_addr := 'Đường Nguyễn Ảnh Thủ, Xã Tân Xuân, Huyện Hóc Môn, TP. Hồ Chí Minh';
SET @site_lat  := 10.8890;
SET @site_lon  := 106.5940;

INSERT INTO campaigns
 (code, title, product_name, description, category_id, season, target_volume_kg, min_threshold_kg, rescue_price_per_kg, deposit_percent,
  publish_at, farm_cutoff_at, cutoff_at, start_date, end_date, finish_date, delivery_date,
  collection_site_name, collection_site_address, collection_site_latitude, collection_site_longitude,
  pickup_enabled, status, booked_volume_kg, created_by, created_at, updated_at)
VALUES
('TEST-HV-01', 'Giải cứu khoai lang mật Hóc Môn', 'Khoai lang mật',
 '<p>Khoai lang mật Hóc Môn trúng mùa, đã đủ người đặt. Hôm nay nhà vườn mang hàng tới điểm tập kết.</p>',
 2, 'Vụ Đông 2026', 4500, 2000, 15000, 0.3000,
 @now - INTERVAL 8 DAY, @now - INTERVAL 4 DAY, @now - INTERVAL 1 DAY, @today, @today, @today + INTERVAL 3 DAY, @today,
 @site_name, @site_addr, @site_lat, @site_lon, 1, 'THRESHOLD_MET', 0, @admin, @now - INTERVAL 9 DAY, @now);

SET @camp := LAST_INSERT_ID();
UPDATE campaigns SET evaluated_at = cutoff_at, call_posted_at = publish_at, published_at = publish_at WHERE campaign_id = @camp;

-- 4 nhà vườn đã được chia đủ 2.890 kg, chưa cân
INSERT INTO campaign_farm_registrations
 (campaign_id, shop_id, surplus_volume_kg, harvest_date, note, status, review_note, reviewed_at, product_name, product_image_urls,
  proposed_price_per_kg, agreed_price_per_kg, price_committed, shelf_life_tier, allocated_volume_kg,
  received_weight_kg, rejected_weight_kg, accepted_weight_kg, quality_passed, intake_note, intake_at,
  payout_amount, paid_out_at, created_at, updated_at)
SELECT @camp, s.id, v.kg, @today - INTERVAL 1 DAY, NULL, 'APPROVED', NULL, @now - INTERVAL 3 DAY, 'Khoai lang mật', '',
       15000, 15000, 1, 'DAYS_5_6', v.kg, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, @now - INTERVAL 6 DAY, @now
FROM users s
JOIN (
    SELECT 'shop1@gmail.com' AS email, 1000 AS kg
    UNION ALL SELECT 'shop2@gmail.com', 800
    UNION ALL SELECT 'shop5@gmail.com', 600
    UNION ALL SELECT 'shop3@gmail.com', 490
) v ON s.email = v.email COLLATE utf8mb4_unicode_ci;

-- 21 đơn đã cọc = 2.890 kg (20 đơn giao tận nơi, trong đó 1 đơn xa mọi hub; 1 đơn tự đến lấy). Cỡ thùng: 15, 10 hoặc 5 kg
DROP TEMPORARY TABLE IF EXISTS tmp_hv_po;
CREATE TEMPORARY TABLE tmp_hv_po (
    n INT, buyer INT, method VARCHAR(10), address VARCHAR(300), ward VARCHAR(150), lat DOUBLE, lon DOUBLE, kg INT
) DEFAULT CHARSET = utf8mb4;
INSERT INTO tmp_hv_po VALUES
( 1, 8,'DELIVERY','20 Tô Ký, Phường Trung Mỹ Tây, Quận 12, TP. Hồ Chí Minh', 'Phường Trung Mỹ Tây', 10.8520,106.6200,150),
( 2, 2,'DELIVERY','66 Trường Chinh, Phường Đông Hưng Thuận, Quận 12, TP. Hồ Chí Minh', 'Phường Đông Hưng Thuận', 10.8390,106.6250,100),
( 3, 5,'DELIVERY','55 Quang Trung, Phường 10, Quận Gò Vấp, TP. Hồ Chí Minh', 'Phường 10', 10.8330,106.6660,150),
( 4, 1,'DELIVERY','14 Nguyễn Oanh, Phường 17, Quận Gò Vấp, TP. Hồ Chí Minh', 'Phường 17', 10.8400,106.6780,130),
( 5, 4,'DELIVERY','120 Phan Huy Ích, Phường 12, Quận Gò Vấp, TP. Hồ Chí Minh', 'Phường 12', 10.8380,106.6580, 70),
( 6, 6,'DELIVERY','9 Hoàng Hoa Thám, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh', 'Phường 13', 10.8030,106.6480,200),
( 7, 3,'DELIVERY','210 Cộng Hòa, Phường 12, Quận Tân Bình, TP. Hồ Chí Minh', 'Phường 12', 10.8040,106.6400, 90),
( 8,10,'DELIVERY','18 Út Tịch, Phường 4, Quận Tân Bình, TP. Hồ Chí Minh', 'Phường 4', 10.7985,106.6500, 60),
( 9, 9,'DELIVERY','45 Tên Lửa, Phường An Lạc A, Quận Bình Tân, TP. Hồ Chí Minh', 'Phường An Lạc A', 10.7500,106.6140,180),
(10, 8,'DELIVERY','12 Hồ Học Lãm, Phường An Lạc, Quận Bình Tân, TP. Hồ Chí Minh', 'Phường An Lạc', 10.7390,106.6110,120),
(11, 7,'DELIVERY','208 Xô Viết Nghệ Tĩnh, Phường 21, Quận Bình Thạnh, TP. Hồ Chí Minh', 'Phường 21', 10.8040,106.7110,180),
(12, 2,'DELIVERY','30 Nơ Trang Long, Phường 14, Quận Bình Thạnh, TP. Hồ Chí Minh', 'Phường 14', 10.8100,106.6980,130),
(13,10,'DELIVERY','100 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh', 'Phường 25', 10.8000,106.7150, 90),
(14, 4,'DELIVERY','12 Đường số 7, Phường Linh Trung, TP. Thủ Đức, TP. Hồ Chí Minh', 'Phường Linh Trung', 10.8620,106.7760,240),
(15, 7,'DELIVERY','88 Phạm Văn Đồng, Phường Linh Đông, TP. Thủ Đức, TP. Hồ Chí Minh', 'Phường Linh Đông', 10.8440,106.7600,150),
(16, 5,'DELIVERY','1 Võ Văn Ngân, Phường Bình Thọ, TP. Thủ Đức, TP. Hồ Chí Minh', 'Phường Bình Thọ', 10.8510,106.7680,110),
(17, 3,'DELIVERY','101 Nguyễn Thị Thập, Phường Tân Phú, Quận 7, TP. Hồ Chí Minh', 'Phường Tân Phú', 10.7390,106.7100,250),
(18, 6,'DELIVERY','30 Nguyễn Hữu Thọ, Phường Tân Hưng, Quận 7, TP. Hồ Chí Minh', 'Phường Tân Hưng', 10.7350,106.7040,250),
(19, 9,'DELIVERY','8 Huỳnh Tấn Phát, Phường Phú Thuận, Quận 7, TP. Hồ Chí Minh', 'Phường Phú Thuận', 10.7420,106.7280,150),
-- Xa mọi hub hơn 20 km: chưa chia hub được, dùng để tạo hub mới hoặc gán vào hub có sẵn
(20, 1,'DELIVERY','5 Lê Duẩn, Thị trấn Long Thành, Tỉnh Đồng Nai', 'Thị trấn Long Thành', 10.7800,106.9500, 60),
(21, 5,'PICKUP',  NULL, NULL, NULL, NULL, 30);

INSERT INTO pre_orders
 (code, campaign_id, buyer_id, crate_size, crate_quantity, total_kg, unit_price, total_amount, deposit_amount, remaining_amount,
  recipient_name, recipient_phone, shipping_address, shipping_latitude, shipping_longitude, fulfillment_method,
  delivery_ward, shipping_distance_km, shipping_base_fee, shipping_fee, shipping_discount_amount, retry_count,
  status, deposit_paid_at, created_at, updated_at)
SELECT CONCAT('TEST-HV-PO-', LPAD(t.n, 2, '0')), @camp, b.id,
       CASE WHEN t.kg % 15 = 0 THEN 'KG_15' WHEN t.kg % 10 = 0 THEN 'KG_10' ELSE 'KG_5' END,
       t.kg DIV (CASE WHEN t.kg % 15 = 0 THEN 15 WHEN t.kg % 10 = 0 THEN 10 ELSE 5 END),
       t.kg, 15000, 15000 * t.kg, 0, 0,
       b.full_name, b.phone_number,
       IF(t.method = 'PICKUP', CONCAT('Nhận tại ', @site_name, ': ', @site_addr), t.address),
       IF(t.method = 'PICKUP', @site_lat, t.lat), IF(t.method = 'PICKUP', @site_lon, t.lon), t.method,
       t.ward,
       IF(t.method = 'PICKUP', NULL, ROUND(ST_Distance_Sphere(POINT(@site_lon, @site_lat), POINT(t.lon, t.lat)) / 1000 * 1.25, 1)),
       0, 0, 0, 0,
       'DEPOSIT_PAID', @now - INTERVAL (50 - t.n) HOUR, @now - INTERVAL (51 - t.n) HOUR, @now
FROM tmp_hv_po t
JOIN users b ON b.email = CONCAT('buyer', t.buyer, '@gmail.com') COLLATE utf8mb4_unicode_ci;

-- Phí gốc theo km (dùng tính cước shipper), phí người mua trả cố định theo kg, cọc 30%
UPDATE pre_orders p
   SET p.shipping_base_fee = CASE WHEN p.shipping_distance_km IS NULL THEN 0
                                  WHEN p.shipping_distance_km < 5 THEN 15000 WHEN p.shipping_distance_km <= 10 THEN 20000
                                  WHEN p.shipping_distance_km <= 20 THEN 30000 ELSE 30000 + ROUND((p.shipping_distance_km - 20) * 3000) END,
       p.shipping_fee = IF(p.fulfillment_method = 'PICKUP' OR p.total_kg >= 50, 0, IF(p.total_kg >= 30, 50000, 70000))
 WHERE p.campaign_id = @camp;
UPDATE pre_orders p
   SET p.deposit_amount = ROUND((p.total_amount + p.shipping_fee) * 0.3),
       p.remaining_amount = p.total_amount + p.shipping_fee - ROUND((p.total_amount + p.shipping_fee) * 0.3)
 WHERE p.campaign_id = @camp;

INSERT INTO campaign_deposits (pre_order_id, amount, status, held_at, created_at, updated_at)
SELECT p.pre_order_id, p.deposit_amount, 'HELD_IN_ESCROW', p.deposit_paid_at, p.created_at, p.updated_at
FROM pre_orders p
WHERE p.campaign_id = @camp;

UPDATE campaigns c
   SET c.booked_volume_kg = (SELECT COALESCE(SUM(p.total_kg), 0) FROM pre_orders p WHERE p.campaign_id = c.campaign_id)
 WHERE c.campaign_id = @camp;

-- Ví sàn đang tạm giữ đúng số tiền cọc của các đơn trên
INSERT INTO wallets (user_id, status, type, total_balance, frozen_balance, total_revenue_all_time, total_withdrawn, commission_wallet, cod_reserved, create_at, update_at)
SELECT @admin, 'ACTIVE', 'PLATFORM', 50000000, (SELECT COALESCE(SUM(amount), 0) FROM campaign_deposits), 0, 0, 0, 0, @now, @now;

DROP TEMPORARY TABLE IF EXISTS tmp_hv_shop;
DROP TEMPORARY TABLE IF EXISTS tmp_hv_product;
DROP TEMPORARY TABLE IF EXISTS tmp_hv_hub;
DROP TEMPORARY TABLE IF EXISTS tmp_hv_buyer;
DROP TEMPORARY TABLE IF EXISTS tmp_hv_ship;
DROP TEMPORARY TABLE IF EXISTS tmp_hv_po;

SET SQL_SAFE_UPDATES = @old_safe;

-- Kết quả mong đợi: 24 hub, 12 shop, 10 người mua, 13 tài xế / shipper, 1 chiến dịch 21 đơn 2.890 kg
SELECT (SELECT COUNT(*) FROM delivery_hubs) AS so_hub,
       (SELECT COUNT(*) FROM users WHERE role_id = 2) AS so_shop,
       (SELECT COUNT(*) FROM users WHERE role_id = 1) AS so_nguoi_mua,
       (SELECT COUNT(*) FROM users WHERE role_id = 3) AS so_tai_xe,
       (SELECT COUNT(*) FROM pre_orders) AS so_don,
       (SELECT booked_volume_kg FROM campaigns WHERE code = 'TEST-HV-01') AS tong_kg;
