-- =====================================================================
-- 天机AI助手 tj-aigc 服务数据库脚本
-- 对应服务：aigc-service（tj-aigc 模块，端口 8094）
-- 数据库连接（课程虚拟机）：192.168.150.101:3306  root / itcast321bca
-- 使用方式：在 MySQL 中直接执行本脚本
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `tj_aigc` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
USE `tj_aigc`;

-- ---------------------------------------------------------------------
-- 对话 session 表（新建会话功能持久化）
-- 实体类：com.tianji.aigc.entity.ChatSession
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_session` (
	`id` BIGINT(19) NOT NULL COMMENT '数据id',
	`session_id` VARCHAR(32) NOT NULL COMMENT '会话id' COLLATE 'utf8mb4_0900_ai_ci',
	`user_id` BIGINT(19) NOT NULL DEFAULT '0' COMMENT '用户id',
	`title` VARCHAR(100) NULL DEFAULT NULL COMMENT '会话标题' COLLATE 'utf8mb4_bin',
	`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
	`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
	`creater` BIGINT(19) NOT NULL COMMENT '创建人',
	`updater` BIGINT(19) NOT NULL COMMENT '更新人',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `session_id_index` (`session_id`) USING BTREE,
	INDEX `user_id_index` (`user_id`) USING BTREE,
	INDEX `update_time_index` (`update_time`) USING BTREE
)
COMMENT='对话session'
COLLATE='utf8mb4_bin'
ENGINE=InnoDB
;
