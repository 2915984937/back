-- ================================================
-- 自习室小程序 · 数据库初始化脚本
-- MySQL 5.7+
-- ================================================

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `openid`           VARCHAR(64)     DEFAULT NULL                     COMMENT '微信 openid，小程序内唯一身份（匿名化后清空）',
  `phone`            VARCHAR(32)     NOT NULL DEFAULT ''              COMMENT '手机号（明文）',
  `nickname`         VARCHAR(64)     NOT NULL DEFAULT ''              COMMENT '昵称',
  `avatar`           VARCHAR(255)    NOT NULL DEFAULT ''              COMMENT '头像 URL',
  `gender`           TINYINT         NOT NULL DEFAULT 0               COMMENT '0-未知 1-男 2-女',
  `balance`          DECIMAL(10,2)   NOT NULL DEFAULT 0.00            COMMENT '账户余额（按需使用）',
  `status`           TINYINT         NOT NULL DEFAULT 0               COMMENT '0-正常 1-永久封禁（人工）',
  `ban_until`        DATETIME        DEFAULT NULL                     COMMENT '临时封禁截止（30天3次超时未到→封7天）',
  `deleted`          TINYINT         NOT NULL DEFAULT 0               COMMENT '0-正常 1-已注销（匿名化，openid/nickname/avatar/phone 已清空）',
  `last_login_time`  DATETIME        DEFAULT NULL                     COMMENT '最近登录时间',
  `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid`    (`openid`),
  KEY `idx_create_time`     (`create_time`),
  KEY `idx_status`          (`status`, `ban_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C端用户表';