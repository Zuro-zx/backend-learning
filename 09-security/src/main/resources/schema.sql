-- ============================================================
-- 09-security 建表脚本（每次启动先删后建，改结构重启即可）
-- 与 05-mybatis 共用一个库 backend_learning，表名统一加 sec_ 前缀
-- 表结构与演示数据来自课程数据表，字段含义见需求文档第 05 节
-- ============================================================

DROP TABLE IF EXISTS `sec_user_role`;
DROP TABLE IF EXISTS `sec_task`;
DROP TABLE IF EXISTS `sec_user`;
DROP TABLE IF EXISTS `sec_role`;

-- 角色字典：三条固定数据，code 与 RoleCode 枚举同名
CREATE TABLE `sec_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(20) NOT NULL COMMENT '角色编码，与 Role 枚举同名：STUDENT / TEACHER / ADMIN',
  `name` varchar(50) NOT NULL COMMENT '角色名称，给页面显示',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色';

-- 账号：password 存 BCrypt 密文（BR-5），enabled 为 0 时登录返回 30002
CREATE TABLE `sec_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '登录名',
  `password` varchar(100) NOT NULL COMMENT 'BCrypt 密文，不可逆',
  `nickname` varchar(50) NOT NULL COMMENT '姓名，页面上显示',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1 启用 / 0 停用，停用账号登录会被拒',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号';

-- 账号与角色多对多：主键是两列组合，删账号要先删这里
CREATE TABLE `sec_user_role` (
  `user_id` bigint unsigned NOT NULL COMMENT '用户 id',
  `role_id` bigint unsigned NOT NULL COMMENT '角色 id',
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与角色的关联';

-- 任务提交：归属指向 sec_user.id（BR-2 存 id 不存用户名）
CREATE TABLE `sec_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(100) NOT NULL COMMENT '任务标题',
  `content` varchar(2000) NOT NULL COMMENT '完成情况说明',
  `owner_id` bigint unsigned NOT NULL COMMENT '提交人，指向 sec_user.id',
  `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED 待批改 / REVIEWED 已批改',
  `score` tinyint unsigned DEFAULT NULL COMMENT '分数 0 到 100，未批改时为空',
  `comment` varchar(500) DEFAULT NULL COMMENT '教师评语',
  `submitted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `reviewed_at` datetime DEFAULT NULL COMMENT '批改时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_owner` (`owner_id`),
  KEY `idx_task_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实训任务提交记录';
