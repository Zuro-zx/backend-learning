-- 测试用预置数据：id=1 供 testSearch / testDelete 使用
INSERT INTO t_user (id, username, password, nickname, gender, age, member_level, points, balance, vip, status,
                    create_time, update_time, deleted, version)
VALUES (1, 'zhangsan', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '张三', 1, 20, 'BRONZE', 0,
        0.00, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0);
