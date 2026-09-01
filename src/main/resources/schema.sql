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
  ban_until        DATETIME        DEFAULT NULL                     COMMENT '临时封禁截止（30天3次超时未到→封7天）',
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
  business_hours VARCHAR(64)     NOT NULL DEFAULT ''                   COMMENT '营业时间，如 08:00-22:00',
  phone          VARCHAR(32)     NOT NULL DEFAULT ''                   COMMENT '联系电话',
  seat_count     INT             NOT NULL DEFAULT 0                    COMMENT '座位总数（冗余，按 seat_count 过滤）',
  layout_image   VARCHAR(255)    NOT NULL DEFAULT ''                   COMMENT '座位平面图底图 URL（前端叠加坐标）',
  status         TINYINT         NOT NULL DEFAULT 0                    COMMENT '0-营业 1-暂停营业 2-关闭',
  sort           INT             NOT NULL DEFAULT 0                    COMMENT '排序（小在前）',
  create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_geo    (latitude, longitude),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室/门店';

CREATE TABLE biz_seat (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT             COMMENT '主键',
  store_id    BIGINT UNSIGNED NOT NULL                             COMMENT '所属自习室',
  seat_no     VARCHAR(16)     NOT NULL                             COMMENT '座位编号，如 A01、B05',
  pos_x       INT             NOT NULL DEFAULT 0                   COMMENT '平面图 X 坐标（像素）',
  pos_y       INT             NOT NULL DEFAULT 0                   COMMENT '平面图 Y 坐标（像素）',
  seat_type   TINYINT         NOT NULL DEFAULT 1                    COMMENT '1-单人 2-双人 3-VIP',
  price_hour  DECIMAL(6,2)    NOT NULL DEFAULT 0.00                 COMMENT '元/小时',
  status      TINYINT         NOT NULL DEFAULT 0                    COMMENT '0-可用 1-维护 2-停用',
  sort        INT             NOT NULL DEFAULT 0,
  create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_store_seat_no (store_id, seat_no),
  INDEX idx_store (store_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位';