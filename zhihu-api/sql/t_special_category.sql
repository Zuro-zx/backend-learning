-- ============================================================
-- 扩展练习 1：为已有数据库补 category 列并打标（增量脚本）
-- 执行方式（Windows）：
--   "C:\Program Files\MySQL\MySQL Server 9.2\bin\mysql.exe" -uroot -p123456 \
--       --default-character-set=utf8mb4 backend_learning < t_special_category.sql
-- 适用场景：表已存在、但还没有 category 列（例如重新导入过旧版 t_special.sql）。
-- 本脚本可重复执行：列 / 索引已存在时自动跳过，只补尚未分类的行。
-- 全新安装直接执行 t_special.sql 即可，那份脚本已内置分类列与打标语句。
-- ============================================================

USE backend_learning;

-- 1. 列不存在才新增（MySQL 9.x 没有 ADD COLUMN IF NOT EXISTS，用 information_schema 判断）
SET @has_column := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_special'
      AND COLUMN_NAME = 'category'
);
SET @ddl := IF(@has_column = 0,
    'ALTER TABLE t_special ADD COLUMN category VARCHAR(20) NULL COMMENT ''专题分类'' AFTER description',
    'SELECT ''category 列已存在，跳过新增'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 索引不存在才创建
SET @has_index := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_special'
      AND INDEX_NAME = 'idx_special_category'
);
SET @ddl := IF(@has_index = 0,
    'CREATE INDEX idx_special_category ON t_special (category)',
    'SELECT ''idx_special_category 已存在，跳过创建'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 只给尚未分类的行打标（顺序即优先级：「网络中国节·端午」必须先被节日规则接住）
UPDATE t_special
SET category = CASE
    WHEN title REGEXP '中秋|七夕|端午|元宵|清明|重阳|春节|元旦|国庆|新岁|龙腾|金蛇|骏马|粽香|月圆|华诞|孝满|忆满'
        THEN '传统节日'
    WHEN title REGEXP '网络|网安|护苗|普法|国安|国家安全|互联网|好网民|清朗'
        THEN '网络文明'
    WHEN title REGEXP '阅读|读书|好书|书香|文化|大视听|视听'
        THEN '阅读文化'
    WHEN title REGEXP '科学|科技|未来|想象'
        THEN '科技前沿'
    WHEN title REGEXP '四川|武汉|两会|经济|改革开放|现代化|亲子|健康|跌倒|大道|潮涌|征程|图景|跟党走|北京|法治|学法'
        THEN '社会民生'
    ELSE '其他'
    END
WHERE category IS NULL;

-- 4. 校验
SELECT category, COUNT(*) AS total
FROM t_special
GROUP BY category
ORDER BY total DESC;

SELECT COUNT(*) AS total_rows,
       COUNT(category) AS categorized_rows
FROM t_special;
