package com.sang.be_capnong.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cho phép một nhà vườn đăng ký nhiều lô hàng vào cùng một chiến dịch:
 * Hibernate ddl-auto=update không tự xoá unique index cũ uk_campaign_farm(campaign_id, shop_id),
 * nên bỏ nó ở đây. Tạo index thường (campaign_id, shop_id) trước để khoá ngoại campaign_id vẫn có index.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignSchemaFixer implements ApplicationRunner {

    private static final String TABLE = "campaign_farm_registrations";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        dropCrateSizeChecks();
        migrateShelfLifeTiers();
        migrateShippingStatus();
        try {
            if (!indexExists("uk_campaign_farm")) {
                return;
            }
            if (!indexExists("idx_cfr_campaign_shop")) {
                jdbcTemplate.execute("CREATE INDEX idx_cfr_campaign_shop ON " + TABLE + " (campaign_id, shop_id)");
            }
            jdbcTemplate.execute("ALTER TABLE " + TABLE + " DROP INDEX uk_campaign_farm");
            log.info("[CampaignSchema] Dropped unique index uk_campaign_farm: shops may now register multiple lots per campaign");
        } catch (Exception e) {
            log.warn("[CampaignSchema] Could not drop uk_campaign_farm: {}", e.getMessage());
        }
    }

    /** Hibernate tạo CHECK (crate_size in (...)) cho enum; thêm thùng 3/5 kg cần bỏ ràng buộc cũ */
    private void dropCrateSizeChecks() {
        try {
            for (String name : jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'pre_orders' AND CONSTRAINT_TYPE = 'CHECK'", String.class)) {
                try {
                    jdbcTemplate.execute("ALTER TABLE pre_orders DROP CHECK `" + name + "`");
                    log.info("[CampaignSchema] Dropped check constraint {} on pre_orders", name);
                } catch (Exception ex) {
                    log.warn("[CampaignSchema] Could not drop check {}: {}", name, ex.getMessage());
                }
            }
            jdbcTemplate.execute("ALTER TABLE pre_orders MODIFY crate_size VARCHAR(16)");
        } catch (Exception e) {
            log.warn("[CampaignSchema] Cannot fix pre_orders.crate_size: {}", e.getMessage());
        }
    }

    /** Mức bảo quản đổi từ 1–2 ngày sang 2–3 ngày (tính từ ngày chốt): bỏ CHECK enum cũ và đổi dữ liệu DAYS_1_2 -> DAYS_2_3 */
    private void migrateShelfLifeTiers() {
        try {
            for (String name : jdbcTemplate.queryForList(
                    "SELECT tc.CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS tc JOIN information_schema.CHECK_CONSTRAINTS cc "
                            + "ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME "
                            + "WHERE tc.TABLE_SCHEMA = DATABASE() AND tc.TABLE_NAME = '" + TABLE + "' AND tc.CONSTRAINT_TYPE = 'CHECK' "
                            + "AND cc.CHECK_CLAUSE LIKE '%DAYS_%'", String.class)) {
                jdbcTemplate.execute("ALTER TABLE " + TABLE + " DROP CHECK `" + name + "`");
                log.info("[CampaignSchema] Dropped check constraint {} on {}", name, TABLE);
            }
            int n = jdbcTemplate.update("UPDATE " + TABLE + " SET shelf_life_tier = 'DAYS_2_3' WHERE shelf_life_tier = 'DAYS_1_2'");
            if (n > 0) log.info("[CampaignSchema] Migrated {} lots DAYS_1_2 -> DAYS_2_3", n);
        } catch (Exception e) {
            log.warn("[CampaignSchema] Cannot migrate shelf_life_tier: {}", e.getMessage());
        }
    }

    /** Thêm trạng thái SHIPPING: đơn shipper đã lấy khỏi hub (READY + picked_up_at) chuyển sang SHIPPING */
    private void migrateShippingStatus() {
        try {
            int n = jdbcTemplate.update("UPDATE pre_orders SET status = 'SHIPPING' WHERE status = 'READY' AND picked_up_at IS NOT NULL");
            if (n > 0) log.info("[CampaignSchema] Migrated {} pre-orders READY -> SHIPPING", n);
        } catch (Exception e) {
            log.warn("[CampaignSchema] Cannot migrate pre_orders SHIPPING status: {}", e.getMessage());
        }
    }

    private boolean indexExists(String name) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class, TABLE, name);
        return n != null && n > 0;
    }
}
