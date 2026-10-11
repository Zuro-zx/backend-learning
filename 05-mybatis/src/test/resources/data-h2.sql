-- 测试用预置数据：id=1 供 testSearch / testDelete 使用
INSERT INTO t_user (id, username, password, nickname, gender, age, member_level, points, balance, vip, status,
                    create_time, update_time, deleted, version)
VALUES (1, 'zhangsan', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '张三', 1, 20, 'BRONZE', 0,
        0.00, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0);

-- 显式指定 id 插入后，H2 的自增序列不会自动前移，这里手动重置，避免 testInsert 生成重复主键
ALTER TABLE t_user ALTER COLUMN id RESTART WITH 100;

-- 课程 / 商品 / 订单预置数据，同样重置自增序列
INSERT INTO t_course (id, name, teacher, credit, create_time, update_time, deleted)
VALUES (1, 'Spring Boot 实战', 'zsj', 3.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
ALTER TABLE t_course ALTER COLUMN id RESTART WITH 100;

INSERT INTO t_product (id, name, price, stock, create_time, update_time)
VALUES (1, '机械键盘', 299.00, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
ALTER TABLE t_product ALTER COLUMN id RESTART WITH 100;

INSERT INTO t_order (id, order_no, user_id, product_id, quantity, amount, create_time, update_time, deleted)
VALUES (1, '20261001080000000001', 1, 1, 1, 299.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
ALTER TABLE t_order ALTER COLUMN id RESTART WITH 100;
