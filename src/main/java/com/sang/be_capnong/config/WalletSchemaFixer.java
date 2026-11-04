package com.sang.be_capnong.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate (ddl-auto: update) đã tạo cột wallet_transactions.type kiểu MySQL ENUM ở bản đầu
 * và không tự sửa khi thêm loại bút toán mới. Đổi 1 lần sang VARCHAR(40) để thêm loại không cần migrate tay.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletSchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String type = jdbcTemplate.query(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'wallet_transactions' AND COLUMN_NAME = 'type'",
                    rs -> rs.next() ? rs.getString(1) : null);
            if ("enum".equalsIgnoreCase(type)) {
                jdbcTemplate.execute("ALTER TABLE wallet_transactions MODIFY COLUMN type VARCHAR(40) NOT NULL");
                log.info("[WalletSchema] wallet_transactions.type: ENUM -> VARCHAR(40)");
            }
        } catch (Exception e) {
            log.warn("[WalletSchema] Cannot check wallet_transactions.type: {}", e.getMessage());
        }
    }
}
