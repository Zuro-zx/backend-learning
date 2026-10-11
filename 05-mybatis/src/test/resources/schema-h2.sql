-- 测试用 H2 内存库建表脚本（类型按 H2 方言调整：TEXT / JSON / ENUM 用 VARCHAR 代替）
DROP TABLE IF EXISTS t_user;

CREATE TABLE t_user
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    password        VARCHAR(64),
    nickname        VARCHAR(50),
    gender          TINYINT      NOT NULL DEFAULT 0,
    age             INT,
    member_level    VARCHAR(20)  NOT NULL DEFAULT 'BRONZE',
    points          INT          NOT NULL DEFAULT 0,
    balance         DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    score           FLOAT,
    phone           VARCHAR(20),
    email           VARCHAR(100),
    avatar          VARCHAR(255),
    intro           VARCHAR(2000),
    tags            VARCHAR(500),
    birthday        DATE,
    work_time       TIME,
    vip             TINYINT      NOT NULL DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 1,
    last_login_time TIMESTAMP,
    create_time     TIMESTAMP    NOT NULL,
    update_time     TIMESTAMP    NOT NULL,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    version         INT          NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS t_course;

CREATE TABLE t_course
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    teacher     VARCHAR(50)  NOT NULL,
    credit      DECIMAL(3, 1) NOT NULL,
    create_time TIMESTAMP    NOT NULL,
    update_time TIMESTAMP    NOT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS t_product;

CREATE TABLE t_product
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,
    stock       INT          NOT NULL,
    create_time TIMESTAMP    NOT NULL,
    update_time TIMESTAMP    NOT NULL
);

DROP TABLE IF EXISTS t_order;

CREATE TABLE t_order
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no    VARCHAR(32)  NOT NULL,
    user_id     BIGINT,
    product_id  BIGINT       NOT NULL,
    quantity    INT          NOT NULL,
    amount      DECIMAL(12, 2) NOT NULL,
    create_time TIMESTAMP    NOT NULL,
    update_time TIMESTAMP    NOT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0
);
