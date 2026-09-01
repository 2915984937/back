-- ================================================
-- 自习室小程序 · 数据库初始化脚本
-- MySQL 5.7+
-- ================================================

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT          COMMENT '主键',
  openid           VARCHAR(64)     DEFAULT NULL                     COMMENT '微信 openid，小程序内唯一身份（匿名化后清空）',
  phone            VARCHAR(32)     NOT NULL DEFAULT ''              COMMENT '手机号（明文）',
  nickname         VARCHAR(64)     NOT NULL DEFAULT ''              COMMENT '昵称',
  avatar           VARCHAR(255)    NOT NULL DEFAULT ''              COMMENT '头像 URL',
  gender           TINYINT         NOT NULL DEFAULT 0               COMMENT '0-未知 1-男 2-女',
  balance          DECIMAL(10,2)   NOT NULL DEFAULT 0.00            COMMENT '账户余额（按需使用）',
  status           TINYINT         NOT NULL DEFAULT 0               COMMENT '0-正常 1-永久封禁（人工）',
  ban_until        DATETIME        DEFAULT NULL                     COMMENT '临时封禁截止（30天3次超时未到->封7天）',
  deleted          TINYINT         NOT NULL DEFAULT 0               COMMENT '0-正常 1-已注销（匿名化，openid/nickname/avatar/phone 已清空）',
  last_login_time  DATETIME        DEFAULT NULL                     COMMENT '最近登录时间',
  create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_openid    (openid),
  KEY idx_create_time     (create_time),
  KEY idx_status          (status, ban_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C端用户表';

-- ================================================
-- 自习室 / 座位（方案 A：单表，无房间概念）
-- ================================================

DROP TABLE IF EXISTS biz_seat;
DROP TABLE IF EXISTS biz_store;

CREATE TABLE biz_store (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT             COMMENT '主键',
  name           VARCHAR(64)     NOT NULL                             COMMENT '自习室名称',
  address        VARCHAR(255)    NOT NULL DEFAULT ''                   COMMENT '详细地址',
  longitude      DECIMAL(10,7)   NOT NULL                             COMMENT '经度',
  latitude       DECIMAL(10,7)   NOT NULL                             COMMENT '纬度',
  cover_image    VARCHAR(255)    NOT NULL DEFAULT ''                   COMMENT '封面图 URL',
  phone          VARCHAR(32)     NOT NULL DEFAULT ''                   COMMENT '联系电话',
  layout_image   VARCHAR(255)    NOT NULL DEFAULT ''                   COMMENT '座位平面图底图 URL（前端叠加坐标）',
  status         TINYINT         NOT NULL DEFAULT 0                    COMMENT '0-营业 1-暂停 2-关闭',
  create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_geo    (latitude, longitude),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室/门店';

CREATE TABLE biz_seat (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT             COMMENT '主键',
  store_id    BIGINT UNSIGNED NOT NULL                             COMMENT '所属自习室',
  seat_name   VARCHAR(32)     NOT NULL DEFAULT ''                   COMMENT '座位显示名，如 A01',
  pos_x       INT             NOT NULL DEFAULT 0                    COMMENT '平面图 X 坐标（像素）',
  pos_y       INT             NOT NULL DEFAULT 0                    COMMENT '平面图 Y 坐标（像素）',
  price_hour  DECIMAL(6,2)    NOT NULL DEFAULT 0.00                 COMMENT '元/小时',
  status      TINYINT         NOT NULL DEFAULT 0                    COMMENT '0-可用 1-维护 2-停用',
  device_id   VARCHAR(64)     NOT NULL DEFAULT ''                   COMMENT '关联 IoT 设备 ID',
  create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_store_seat (store_id, seat_name),
  INDEX idx_store (store_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位';

-- ================================================
-- 测试数据
-- ================================================

INSERT INTO biz_store (id, name, address, longitude, latitude, cover_image, phone, layout_image, status) VALUES
(1, '知学自习室(中关村店)', '北京市海淀区中关村大街1号 海龙大厦 6F', 116.3154732, 39.9832461, '', '010-88880001', '', 0),
(2, '知学自习室(望京店)',   '北京市朝阳区望京SOHO T3 18F',           116.4814903, 39.9963582, '', '010-88880002', '', 0),
(3, '知学自习室(国贸店)',   '北京市朝阳区建国路88号 SOHO现代城 A座',  116.4604382, 39.9070578, '', '010-88880003', '', 1);

-- 门店1: 3行 x 3列 = 9 个座位 + 2 个 VIP 座（价格区分）
INSERT INTO biz_seat (store_id, seat_name, pos_x, pos_y, price_hour, status, device_id) VALUES
(1, 'A01', 100, 100, 12.00, 0, ''),
(1, 'A02', 200, 100, 12.00, 0, ''),
(1, 'A03', 300, 100, 12.00, 1, ''),
(1, 'B01', 100, 200, 12.00, 0, ''),
(1, 'B02', 200, 200, 12.00, 0, ''),
(1, 'B03', 300, 200, 12.00, 0, ''),
(1, 'C01', 100, 300, 12.00, 0, ''),
(1, 'C02', 200, 300, 12.00, 0, ''),
(1, 'C03', 300, 300, 12.00, 0, ''),
(1, 'V01', 450, 100, 25.00, 0, 'dev-vip-01'),
(1, 'V02', 450, 200, 25.00, 0, 'dev-vip-02');

-- 门店2: 4行 x 4列 = 16 个座位
INSERT INTO biz_seat (store_id, seat_name, pos_x, pos_y, price_hour, status, device_id) VALUES
(2, 'A01', 80,  80,  10.00, 0, ''),
(2, 'A02', 160, 80,  10.00, 0, ''),
(2, 'A03', 240, 80,  10.00, 2, ''),
(2, 'A04', 320, 80,  10.00, 0, ''),
(2, 'B01', 80,  160, 10.00, 0, ''),
(2, 'B02', 160, 160, 18.00, 0, ''),
(2, 'B03', 240, 160, 18.00, 0, ''),
(2, 'B04', 320, 160, 10.00, 0, ''),
(2, 'C01', 80,  240, 10.00, 0, ''),
(2, 'C02', 160, 240, 10.00, 0, ''),
(2, 'C03', 240, 240, 10.00, 0, ''),
(2, 'C04', 320, 240, 10.00, 0, ''),
(2, 'D01', 80,  320, 10.00, 0, ''),
(2, 'D02', 160, 320, 10.00, 0, ''),
(2, 'D03', 240, 320, 10.00, 0, ''),
(2, 'D04', 320, 320, 10.00, 0, '');

-- 门店3: 暂停营业
INSERT INTO biz_seat (store_id, seat_name, pos_x, pos_y, price_hour, status, device_id) VALUES
(3, 'A01', 100, 100, 15.00, 0, ''),
(3, 'A02', 200, 100, 15.00, 0, '');