-- ============================================================
-- 08-security 演示数据：六个账号 + 角色 / 权限点
-- 密码统一 123456，存同一段 {bcrypt} 密文（BCrypt 自带随机盐，同一明文密文不同，
-- 这里为了脚本简短复用同一串，校验结果一致）
-- ============================================================

INSERT INTO sec_user (id, username, phone, password, nickname, enabled, locked)
VALUES (1, 'alice', '13800000001',
        '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '爱丽丝', TRUE, FALSE),
       (2, 'bob', '13800000002',
        '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '鲍勃', TRUE, FALSE),
       (3, 'carol', '13800000003',
        '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '卡罗尔', TRUE, FALSE),
       (4, 'dave', '13800000004',
        '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '戴夫', FALSE, FALSE),
       (5, 'frank', '13800000005',
        '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '弗兰克', TRUE, TRUE),
       (6, 'erin', '13800000006',
        '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '艾琳', TRUE, FALSE);

-- grant_type=ROLE 时 toAuthority() 补 ROLE_ 前缀；=PERM 时原样返回
INSERT INTO sec_role (user_id, role_code, grant_type)
VALUES (1, 'USER', 'ROLE'),
       (1, 'ARTICLE_DELETE', 'PERM'),
       (2, 'ADMIN', 'ROLE'),
       (2, 'AUDITOR', 'ROLE'),
       (3, 'ADMIN', 'ROLE'),
       (4, 'USER', 'ROLE'),
       (5, 'USER', 'ROLE'),
       (6, 'AUDITOR', 'ROLE');
