-- 示例数据：4 条可查到的用户 + 1 条 deleted = 1 的记录
-- password 统一为 123456 的 SHA-256 摘要
INSERT INTO t_user (id, username, password, nickname, gender, age, member_level, points, balance, score,
                    phone, email, avatar, intro, tags, birthday, work_time, vip, status, last_login_time,
                    create_time, update_time, deleted, version)
VALUES (1, 'zhangsan', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '张三', 1, 20, 'GOLD', 1200,
        1500.50, 4.8, '13800000001', 'zhangsan@example.com', '/avatar/1.png', '后端开发，喜欢写 Java',
        '["Java","篮球"]', '2000-01-01', '09:00:00', 1, 1, '2026-10-01 08:30:00',
        '2026-10-01 08:00:00', '2026-10-01 08:30:00', 0, 0),
       (2, 'lisi', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '李四', 1, 25, 'SILVER', 800,
        320.00, 4.2, '13800000002', 'lisi@example.com', '/avatar/2.png', '前端开发，喜欢篮球',
        '["篮球"]', '1999-05-20', '10:00:00', 0, 1, '2026-10-02 09:15:00',
        '2026-10-01 09:00:00', '2026-10-02 09:15:00', 0, 0),
       (3, 'wangwu', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '王五', 2, 30, 'BRONZE', 1500,
        88.88, 4.5, '13800000003', 'wangwu@example.com', '/avatar/3.png', '测试开发，喜欢旅游和 Java',
        '["Java","旅游"]', '1994-08-08', '08:30:00', 1, 1, '2026-10-03 10:00:00',
        '2026-10-01 10:00:00', '2026-10-03 10:00:00', 0, 0),
       (4, 'zhaoliu', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '赵六', 2, 22, 'SILVER', 300,
        0.00, 3.9, '13800000004', 'zhaoliu@example.com', NULL, '在读学生', '[]', '2004-03-15', '08:00:00', 0, 1,
        NULL, '2026-10-01 11:00:00', '2026-10-01 11:00:00', 0, 0),
       (5, 'sunqi', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '孙七', 1, 28, 'BRONZE', 60,
        12.34, 3.2, '13800000005', 'sunqi@example.com', NULL, '刚转行做后端', '["Java"]', '1998-11-11', '09:30:00', 0, 0,
        NULL, '2026-10-01 12:00:00', '2026-10-01 12:00:00', 0, 0),
       (6, 'deleteduser', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '已删除用户', 0, NULL,
        'BRONZE', 10, 0.00, 3.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 1, NULL,
        '2026-10-01 13:00:00', '2026-10-01 13:00:00', 1, 0);

-- 课程示例数据
INSERT INTO t_course (id, name, teacher, credit, create_time, update_time, deleted)
VALUES (1, 'Spring Boot 实战', 'zsj', 3.0, '2026-10-01 08:00:00', '2026-10-01 08:00:00', 0),
       (2, 'MySQL 从入门到调优', 'mali', 2.5, '2026-10-01 09:00:00', '2026-10-01 09:00:00', 0),
       (3, 'Redis 缓存设计', 'zhaoqiang', 2.0, '2026-10-01 10:00:00', '2026-10-01 10:00:00', 0),
       (4, 'Java 并发编程', 'sunli', 3.5, '2026-10-01 11:00:00', '2026-10-01 11:00:00', 0);

-- 商品示例数据（库存来源）
INSERT INTO t_product (id, name, price, stock, create_time, update_time)
VALUES (1, '机械键盘', 299.00, 100, '2026-10-01 08:00:00', '2026-10-01 08:00:00'),
       (2, '显示器', 1299.00, 50, '2026-10-01 08:00:00', '2026-10-01 08:00:00');
