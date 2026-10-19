-- ============================================================
-- 扩展练习 1：为 t_special 增加分类字段并完成标注
-- 执行方式（Windows）：
--   "C:\Program Files\MySQL\MySQL Server 9.2\bin\mysql.exe" -uroot -p123456 \
--       --default-character-set=utf8mb4 backend_learning < t_special_category.sql
-- 说明：MySQL 9.x 不支持 ADD COLUMN IF NOT EXISTS，本脚本只执行一次；
--       重复执行会因列名重复报 1060，属预期行为。
-- ============================================================

USE backend_learning;

-- 1. 新增分类列，放在 description 之后，便于在 Navicat 中查看
ALTER TABLE t_special
    ADD COLUMN category VARCHAR(20) NULL COMMENT '专题分类' AFTER description;

-- 2. 分类列经常出现在 WHERE 条件里，建普通索引
CREATE INDEX idx_special_category ON t_special (category);

-- 3. 按标题关键词打标。
--    知乎的专题标题多数带活动名（如「网络中国节·端午」），关键词规则比纯数值阈值更贴近语义；
--    先做关键词匹配，匹配不到的再兜底（这里 50 条数据全部能被覆盖）。
--    顺序有意义：先判定节日类，避免「网络中国节·端午」被「网络」抢先归入网络文明。
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

-- 4. 校验：各分类条数与总数
SELECT category, COUNT(*) AS total
FROM t_special
GROUP BY category
ORDER BY total DESC;

SELECT COUNT(*) AS total_rows,
       COUNT(category) AS categorized_rows
FROM t_special;
