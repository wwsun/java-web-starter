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
VALUES ('admin', '$2a$10$k9X1JcFuWv9xw/jT6gSNR.3qCt9NYkOrKphmf6nLxHqgiRPNLCsM.', '管理员', 1);

-- -------------------------------------------
-- RBAC: roles
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `roles` (
    `id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(50) NOT NULL                COMMENT '角色编码，如 ADMIN / USER',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表';

-- -------------------------------------------
-- RBAC: user_roles
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户角色关联表';

-- -------------------------------------------
-- Seed: roles
-- -------------------------------------------
INSERT IGNORE INTO `roles` (`code`) VALUES ('ADMIN'), ('USER');

-- -------------------------------------------
-- Seed: admin user gets ADMIN role
-- （依赖 users 表中 admin 的 id=1）
-- -------------------------------------------
INSERT IGNORE INTO `user_roles` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `users` u, `roles` r
WHERE u.username = 'admin' AND r.code = 'ADMIN';
