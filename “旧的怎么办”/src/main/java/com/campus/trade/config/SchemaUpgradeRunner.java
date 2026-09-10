package com.campus.trade.config;

import com.campus.trade.common.PasswordCodec;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaUpgradeRunner {
    private static final String ADMIN_OPENID = "web_admin";
    private static final String ADMIN_NICKNAME = "admin";
    private static final String ADMIN_PASSWORD = "123456";
    private final JdbcTemplate jdbcTemplate;

    public SchemaUpgradeRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void upgrade() {
        ensureColumnExists("user_auth", "salt", "ALTER TABLE user_auth ADD COLUMN salt VARCHAR(64) NULL");
        ensureColumnExists("user_auth", "password_md5", "ALTER TABLE user_auth ADD COLUMN password_md5 VARCHAR(64) NULL");
        renameColumnIfNeeded("message", "sender_id", "from_user", "BIGINT NOT NULL");
        renameColumnIfNeeded("message", "receiver_id", "to_user", "BIGINT NOT NULL");
        renameColumnIfNeeded("message", "is_read", "read_status", "TINYINT NOT NULL DEFAULT 0");
        ensureColumnExists("message", "conversation_id", "ALTER TABLE message ADD COLUMN conversation_id BIGINT NULL");
        ensureColumnExists("message", "order_id", "ALTER TABLE message ADD COLUMN order_id BIGINT NULL");
        ensureColumnExists("message", "item_id", "ALTER TABLE message ADD COLUMN item_id BIGINT NULL");
        ensureColumnExists("trade_order", "receiver_name", "ALTER TABLE trade_order ADD COLUMN receiver_name VARCHAR(64) NULL");
        ensureColumnExists("trade_order", "receiver_phone", "ALTER TABLE trade_order ADD COLUMN receiver_phone VARCHAR(32) NULL");
        ensureColumnExists("trade_order", "receiver_campus", "ALTER TABLE trade_order ADD COLUMN receiver_campus VARCHAR(64) NULL");
        ensureColumnExists("trade_order", "receiver_detail", "ALTER TABLE trade_order ADD COLUMN receiver_detail VARCHAR(255) NULL");
        ensureColumnExists("trade_order", "payment_method", "ALTER TABLE trade_order ADD COLUMN payment_method VARCHAR(24) NULL");
        ensureColumnExists("trade_order", "payment_no", "ALTER TABLE trade_order ADD COLUMN payment_no VARCHAR(64) NULL");
        ensureColumnExists("trade_order", "cancel_reason", "ALTER TABLE trade_order ADD COLUMN cancel_reason VARCHAR(255) NULL");
        ensureColumnExists("trade_order", "paid_at", "ALTER TABLE trade_order ADD COLUMN paid_at DATETIME NULL");
        ensureColumnExists("trade_order", "shipped_at", "ALTER TABLE trade_order ADD COLUMN shipped_at DATETIME NULL");
        ensureColumnExists("trade_order", "completed_at", "ALTER TABLE trade_order ADD COLUMN completed_at DATETIME NULL");
        ensureColumnExists("dispute_record", "order_status_snapshot", "ALTER TABLE dispute_record ADD COLUMN order_status_snapshot VARCHAR(24) NULL");
        ensureColumnExists("dispute_record", "handler_id", "ALTER TABLE dispute_record ADD COLUMN handler_id BIGINT NULL");
        ensureColumnExists("dispute_record", "handled_at", "ALTER TABLE dispute_record ADD COLUMN handled_at DATETIME NULL");
        dropConstraintIfExists("message", "message_ibfk_1");
        dropConstraintIfExists("message", "message_ibfk_2");
        jdbcTemplate.execute("""
                UPDATE trade_order o
                LEFT JOIN user_account u ON u.id = o.buyer_id
                SET
                  o.receiver_name = COALESCE(NULLIF(o.receiver_name, ''), u.nickname, '待补充'),
                  o.receiver_phone = COALESCE(NULLIF(o.receiver_phone, ''), '未填写'),
                  o.receiver_campus = COALESCE(NULLIF(o.receiver_campus, ''), '望江校区'),
                  o.receiver_detail = COALESCE(NULLIF(o.receiver_detail, ''), '请联系买家确认收货地址')
                """);
        jdbcTemplate.execute("""
                UPDATE trade_order
                SET status = 'PENDING_PAYMENT'
                WHERE status = 'CREATED'
                """);
        jdbcTemplate.execute("ALTER TABLE trade_order MODIFY COLUMN receiver_name VARCHAR(64) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE trade_order MODIFY COLUMN receiver_phone VARCHAR(32) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE trade_order MODIFY COLUMN receiver_campus VARCHAR(64) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE trade_order MODIFY COLUMN receiver_detail VARCHAR(255) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE trade_order MODIFY COLUMN status VARCHAR(24) NOT NULL DEFAULT 'PENDING_PAYMENT'");
        jdbcTemplate.execute("""
                UPDATE dispute_record d
                LEFT JOIN trade_order o ON o.id = d.order_id
                SET d.order_status_snapshot = COALESCE(NULLIF(d.order_status_snapshot, ''), o.status, 'PAID')
                """);
        jdbcTemplate.execute("""
                INSERT INTO conversation(user_a, user_b, last_message_at, created_at)
                SELECT
                  LEAST(m.from_user, m.to_user) AS user_a,
                  GREATEST(m.from_user, m.to_user) AS user_b,
                  MAX(m.created_at) AS last_message_at,
                  MIN(m.created_at) AS created_at
                FROM message m
                LEFT JOIN conversation c
                  ON ((c.user_a = m.from_user AND c.user_b = m.to_user)
                   OR (c.user_a = m.to_user AND c.user_b = m.from_user))
                WHERE m.from_user IS NOT NULL
                  AND m.to_user IS NOT NULL
                  AND c.id IS NULL
                GROUP BY LEAST(m.from_user, m.to_user), GREATEST(m.from_user, m.to_user)
                """);
        jdbcTemplate.execute("""
                UPDATE message m
                JOIN conversation c
                  ON ((c.user_a = m.from_user AND c.user_b = m.to_user)
                   OR (c.user_a = m.to_user AND c.user_b = m.from_user))
                SET m.conversation_id = c.id
                WHERE m.conversation_id IS NULL
                """);
        jdbcTemplate.execute("ALTER TABLE message MODIFY COLUMN conversation_id BIGINT NOT NULL");
        jdbcTemplate.execute("ALTER TABLE dispute_record MODIFY COLUMN order_status_snapshot VARCHAR(24) NOT NULL");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_hidden_order (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  order_id BIGINT NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE KEY uk_hidden_order_user_order (user_id, order_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_hidden_thread (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  order_id BIGINT NOT NULL DEFAULT 0,
                  item_id BIGINT NOT NULL DEFAULT 0,
                  hidden_before DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  UNIQUE KEY uk_hidden_thread_user_scope (user_id, order_id, item_id)
                )
                """);
        ensureAdminAccount();
    }

    private void ensureAdminAccount() {
        Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_account WHERE openid = ?",
                Integer.class,
                ADMIN_OPENID
        );
        if (adminCount == null || adminCount == 0) {
            jdbcTemplate.update(
                    """
                    INSERT INTO user_account(openid, nickname, status, created_at, updated_at)
                    VALUES(?, ?, 'ACTIVE', NOW(), NOW())
                    """,
                    ADMIN_OPENID,
                    ADMIN_NICKNAME
            );
        } else {
            jdbcTemplate.update(
                    """
                    UPDATE user_account
                    SET nickname = ?, status = 'ACTIVE', updated_at = NOW()
                    WHERE openid = ?
                    """,
                    ADMIN_NICKNAME,
                    ADMIN_OPENID
            );
        }
        Long adminUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM user_account WHERE openid = ? LIMIT 1",
                Long.class,
                ADMIN_OPENID
        );
        if (adminUserId == null) {
            return;
        }
        String salt = "campus-admin-salt";
        String passwordHash = PasswordCodec.encode(ADMIN_PASSWORD, salt);
        jdbcTemplate.update(
                """
                INSERT INTO user_auth(user_id, salt, password_md5, created_at, updated_at)
                VALUES(?, ?, ?, NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                  salt = VALUES(salt),
                  password_md5 = VALUES(password_md5),
                  updated_at = NOW()
                """,
                adminUserId,
                salt,
                passwordHash
        );
    }

    private void ensureColumnExists(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void renameColumnIfNeeded(String tableName, String oldColumnName, String newColumnName, String definition) {
        if (hasColumn(tableName, newColumnName) || !hasColumn(tableName, oldColumnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " CHANGE COLUMN " + oldColumnName + " " + newColumnName + " " + definition);
    }

    private boolean hasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private void dropConstraintIfExists(String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND CONSTRAINT_NAME = ?
                  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
                """,
                Integer.class,
                tableName,
                constraintName
        );
        if (count != null && count > 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName);
        }
    }
}
