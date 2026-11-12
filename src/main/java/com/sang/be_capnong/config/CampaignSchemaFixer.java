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

    private boolean indexExists(String name) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class, TABLE, name);
        return n != null && n > 0;
    }
}
