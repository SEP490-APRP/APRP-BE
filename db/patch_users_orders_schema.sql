-- Select the database used by the backend before running this patch.
-- Run after the schema has been imported. Existing rows are preserved.

SET @capnong_schema_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'google_id'
), 'SELECT 1', 'ALTER TABLE users ADD COLUMN google_id VARCHAR(255) UNIQUE');
PREPARE capnong_schema_stmt FROM @capnong_schema_sql;
EXECUTE capnong_schema_stmt;
DEALLOCATE PREPARE capnong_schema_stmt;

SET @capnong_schema_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'cccd'
), 'SELECT 1', 'ALTER TABLE users ADD COLUMN cccd VARCHAR(20)');
PREPARE capnong_schema_stmt FROM @capnong_schema_sql;
EXECUTE capnong_schema_stmt;
DEALLOCATE PREPARE capnong_schema_stmt;

SET @capnong_schema_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'completed_at'
), 'SELECT 1', 'ALTER TABLE orders ADD COLUMN completed_at DATETIME(6)');
PREPARE capnong_schema_stmt FROM @capnong_schema_sql;
EXECUTE capnong_schema_stmt;
DEALLOCATE PREPARE capnong_schema_stmt;

SET @capnong_schema_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'settled_at'
), 'SELECT 1', 'ALTER TABLE orders ADD COLUMN settled_at DATETIME(6)');
PREPARE capnong_schema_stmt FROM @capnong_schema_sql;
EXECUTE capnong_schema_stmt;
DEALLOCATE PREPARE capnong_schema_stmt;

SET @capnong_schema_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'ready_at'
), 'SELECT 1', 'ALTER TABLE orders ADD COLUMN ready_at DATETIME(6)');
PREPARE capnong_schema_stmt FROM @capnong_schema_sql;
EXECUTE capnong_schema_stmt;
DEALLOCATE PREPARE capnong_schema_stmt;

SET @capnong_schema_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'dispatch_wave'
), 'SELECT 1', 'ALTER TABLE orders ADD COLUMN dispatch_wave INT');
PREPARE capnong_schema_stmt FROM @capnong_schema_sql;
EXECUTE capnong_schema_stmt;
DEALLOCATE PREPARE capnong_schema_stmt;

SET @capnong_schema_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'last_dispatched_at'
), 'SELECT 1', 'ALTER TABLE orders ADD COLUMN last_dispatched_at DATETIME(6)');
PREPARE capnong_schema_stmt FROM @capnong_schema_sql;
EXECUTE capnong_schema_stmt;
DEALLOCATE PREPARE capnong_schema_stmt;

SET @capnong_schema_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'dispatch_alerted_at'
), 'SELECT 1', 'ALTER TABLE orders ADD COLUMN dispatch_alerted_at DATETIME(6)');
PREPARE capnong_schema_stmt FROM @capnong_schema_sql;
EXECUTE capnong_schema_stmt;
DEALLOCATE PREPARE capnong_schema_stmt;

