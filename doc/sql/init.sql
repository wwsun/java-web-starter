-- ============================================
-- Database Initialization Script
-- java-web-starter
-- ============================================

CREATE DATABASE IF NOT EXISTS `starter_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `starter_db`;

-- -------------------------------------------
-- Table: users
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`   VARCHAR(64)  NOT NULL                COMMENT '用户名',
    `password`   VARCHAR(255) NOT NULL                COMMENT '密码（BCrypt加密）',
    `nickname`   VARCHAR(64)  NULL                    COMMENT '昵称',
    `email`      VARCHAR(128) NULL                    COMMENT '邮箱',
    `phone`      VARCHAR(20)  NULL                    COMMENT '手机号',
    `avatar`     VARCHAR(255) NULL                    COMMENT '头像URL',
    `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-禁用 1-正常',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_username` (`username`),
    INDEX `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户表';

-- -------------------------------------------
-- Seed data: default admin user
-- Password: admin123 (BCrypt encoded)
-- -------------------------------------------
INSERT INTO `users` (`username`, `password`, `nickname`, `status`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 1);
