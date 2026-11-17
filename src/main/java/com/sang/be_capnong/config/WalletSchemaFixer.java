package com.sang.be_capnong.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tự động sửa schema MySQL để tránh lỗi enum / check constraint / unique index:
 * 1. MySQL 8 tự động sinh Check constraint (vd: wallet_transactions_chk_1) cho các cột @Enumerated,
 *    khiến các enum mới thêm vào (BUYER_REFUND, SHOP_REFUND_DEDUCT, v.v.) bị vi phạm ràng buộc.
 * 2. Cột type cần là VARCHAR(50) và DROP toàn bộ CHECK constraints trên các bảng liên quan.
 * 3. Bỏ unique index uk_wallet_tx_wallet_order_type để cho phép hoàn tiền từng món (partial refund).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletSchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        fixWalletTransactions();
        fixReturnRequests();
        fixEnumColumns();
    }

    private void fixWalletTransactions() {
        // 1. Drop mọi CHECK constraint trên bảng wallet_transactions (vd: wallet_transactions_chk_1)
        try {
            List<String> checkConstraints = jdbcTemplate.query(
                    "SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_transactions' "
                            + "AND CONSTRAINT_TYPE = 'CHECK'",
                    (rs, rowNum) -> rs.getString("CONSTRAINT_NAME"));

            for (String constraintName : checkConstraints) {
                try {
                    jdbcTemplate.execute("ALTER TABLE wallet_transactions DROP CHECK `" + constraintName + "`");
                    log.info("[WalletSchema] Successfully dropped check constraint: {}", constraintName);
                } catch (Exception ex) {
                    try {
                        jdbcTemplate.execute("ALTER TABLE wallet_transactions DROP CONSTRAINT `" + constraintName + "`");
                        log.info("[WalletSchema] Dropped constraint via DROP CONSTRAINT: {}", constraintName);
                    } catch (Exception ignored) {
                        log.warn("[WalletSchema] Could not drop check constraint {}: {}", constraintName, ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[WalletSchema] Cannot inspect TABLE_CONSTRAINTS for wallet_transactions: {}", e.getMessage());
        }

        // Drop tường minh wallet_transactions_chk_1 đề phòng thông tin trong information_schema bị cache
        try {
            jdbcTemplate.execute("ALTER TABLE wallet_transactions DROP CHECK `wallet_transactions_chk_1`");
            log.info("[WalletSchema] Explicitly dropped wallet_transactions_chk_1");
        } catch (Exception ignored) {
        }

        // 2. Chuyển cột type sang VARCHAR(50) NOT NULL
        try {
            jdbcTemplate.execute("ALTER TABLE wallet_transactions MODIFY COLUMN type VARCHAR(50) NOT NULL");
            log.info("[WalletSchema] wallet_transactions.type ensured VARCHAR(50) NOT NULL");
        } catch (Exception e) {
            log.warn("[WalletSchema] Cannot alter wallet_transactions.type: {}", e.getMessage());
        }

        // 3. Drop unique index uk_wallet_tx_wallet_order_type để cho phép hoàn trả nhiều món trong 1 đơn
        try {
            jdbcTemplate.execute("ALTER TABLE wallet_transactions DROP INDEX `uk_wallet_tx_wallet_order_type`");
            log.info("[WalletSchema] Dropped index uk_wallet_tx_wallet_order_type for partial refund support");
        } catch (Exception ignored) {
        }
    }

    /**
     * db/sep.sql tạo một số cột trạng thái bằng ENUM của MySQL với giá trị cũ, lệch enum Java
     * (vd. orders.cod_payment_status chỉ nhận UNPAID/PARTIALLY_PAID/PAID/REFUNDED, code ghi PENDING/CONFIRMED/SETTLED).
     * Ghi giá trị ngoài danh sách sẽ lỗi "Data truncated". Đổi sang VARCHAR để khớp @Enumerated(STRING).
     */
    private void fixEnumColumns() {
        String[][] columns = {
                {"orders", "cod_payment_status", "VARCHAR(32) NULL"},
                {"orders", "cod_payment_option", "VARCHAR(32) NULL"},
                {"wallets", "status", "VARCHAR(16) NULL"},
        };
        for (String[] c : columns) {
            try {
                String type = jdbcTemplate.queryForObject(
                        "SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                                + "AND TABLE_NAME = ? AND COLUMN_NAME = ?", String.class, c[0], c[1]);
                if ("enum".equalsIgnoreCase(type)) {
                    jdbcTemplate.execute("ALTER TABLE " + c[0] + " MODIFY COLUMN " + c[1] + " " + c[2]);
                    log.info("[WalletSchema] {}.{} changed from ENUM to {}", c[0], c[1], c[2]);
                }
            } catch (Exception e) {
                log.warn("[WalletSchema] Cannot fix {}.{}: {}", c[0], c[1], e.getMessage());
            }
        }
    }

    private void fixReturnRequests() {
        try {
            List<String> checkConstraints = jdbcTemplate.query(
                    "SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'return_requests' "
                            + "AND CONSTRAINT_TYPE = 'CHECK'",
                    (rs, rowNum) -> rs.getString("CONSTRAINT_NAME"));

            for (String constraintName : checkConstraints) {
                try {
                    jdbcTemplate.execute("ALTER TABLE return_requests DROP CHECK `" + constraintName + "`");
                    log.info("[ReturnRequestSchema] Dropped check constraint: {}", constraintName);
                } catch (Exception ex) {
                    try {
                        jdbcTemplate.execute("ALTER TABLE return_requests DROP CONSTRAINT `" + constraintName + "`");
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.debug("[ReturnRequestSchema] Cannot inspect constraints for return_requests: {}", e.getMessage());
        }
    }
}
