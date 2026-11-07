-- ============================================================
-- 08-security 建表脚本（每次启动先删后建，改结构重启即可）
-- 库：security_practice（独立库，避免与本机 09-security 的 sec_user / sec_role 冲突）
-- 教程原始脚本表名与字段保持一致，便于对照
-- ============================================================

DROP TABLE IF EXISTS `sec_role`;
DROP TABLE IF EXISTS `sec_user`;

CREATE TABLE sec_user
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录名，密码登录用它',
    phone       VARCHAR(20) DEFAULT NULL UNIQUE COMMENT '手机号，短信登录用它',
    password    VARCHAR(128) NOT NULL COMMENT '带算法前缀的密文，如 {bcrypt}$2a$10$...',
    nickname    VARCHAR(64) DEFAULT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE COMMENT 'false 时登录抛 DisabledException',
    locked      BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'true 时登录抛 LockedException',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '账号';

CREATE TABLE sec_role
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    role_code  VARCHAR(64) NOT NULL,
    grant_type VARCHAR(8)  NOT NULL DEFAULT 'ROLE',
    UNIQUE KEY uk_user_role (user_id, role_code),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '账号的角色与权限点';
