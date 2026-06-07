-- =============================================
-- 贪吃蛇大作战 - 数据库初始化脚本（合并版）
-- 数据库: snake_game (MySQL 8.0+)
-- 说明：已包含 migration_v2 修复（game.room_id → VARCHAR）
--       默认用户（admin/player1/test）由 DataInitializer.java 在启动时自动生成
-- =============================================

-- ----------------------------
-- 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '加密密码 (BCrypt)',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT '' COMMENT '头像URL',
    level INT NOT NULL DEFAULT 1 COMMENT '等级',
    total_score BIGINT NOT NULL DEFAULT 0 COMMENT '累计总分',
    total_games INT NOT NULL DEFAULT 0 COMMENT '总局数',
    wins INT NOT NULL DEFAULT 0 COMMENT '胜场数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 房间表
-- ----------------------------
CREATE TABLE IF NOT EXISTS room (
    id VARCHAR(64) NOT NULL COMMENT '房间ID',
    name VARCHAR(100) NOT NULL COMMENT '房间名称',
    host_id BIGINT DEFAULT NULL COMMENT '房主ID',
    game_mode VARCHAR(10) NOT NULL DEFAULT 'multi' COMMENT '游戏模式: single/multi',
    max_players INT NOT NULL DEFAULT 6 COMMENT '最大人数',
    game_duration INT NOT NULL DEFAULT 300 COMMENT '游戏时长(秒), 单人模式=0',
    has_password TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否有密码',
    password VARCHAR(50) DEFAULT NULL COMMENT '房间密码',
    allow_bots TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许机器人',
    status VARCHAR(10) NOT NULL DEFAULT 'waiting' COMMENT '房间状态: waiting/playing/finished',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_host_id (host_id),
    KEY idx_status (status),
    KEY idx_game_mode (game_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间表';

-- ----------------------------
-- 房间玩家关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS room_player (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    room_id VARCHAR(64) NOT NULL COMMENT '房间ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    is_host TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为房主',
    is_ready TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否准备就绪',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_user (room_id, user_id),
    KEY idx_room_id (room_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间玩家关联表';

-- ----------------------------
-- 游戏会话表（已修复: room_id 为 VARCHAR(64)）
-- ----------------------------
CREATE TABLE IF NOT EXISTS game (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '游戏ID',
    room_id VARCHAR(64) DEFAULT NULL COMMENT '所属房间ID',
    game_mode VARCHAR(10) NOT NULL COMMENT '游戏模式: single/multi',
    duration INT NOT NULL DEFAULT 0 COMMENT '实际游戏时长(秒)',
    player_count INT NOT NULL DEFAULT 0 COMMENT '参与人数',
    status VARCHAR(10) NOT NULL DEFAULT 'finished' COMMENT '游戏状态',
    started_at DATETIME DEFAULT NULL COMMENT '开始时间',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id),
    KEY idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏会话表';

-- ----------------------------
-- 玩家成绩表
-- ----------------------------
CREATE TABLE IF NOT EXISTS game_player_result (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    game_id BIGINT NOT NULL COMMENT '游戏ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    score INT NOT NULL DEFAULT 0 COMMENT '得分',
    kills INT NOT NULL DEFAULT 0 COMMENT '击杀数(多人)',
    snake_length INT NOT NULL DEFAULT 0 COMMENT '蛇最大长度',
    survival_time DOUBLE NOT NULL DEFAULT 0 COMMENT '存活时间(秒)',
    is_alive TINYINT(1) NOT NULL DEFAULT 1 COMMENT '最终是否存活',
    is_bot TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为机器人',
    `rank` INT NOT NULL DEFAULT 0 COMMENT '最终排名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_user (game_id, user_id),
    KEY idx_game_id (game_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家成绩表';

-- ----------------------------
-- 游戏事件日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS game_event_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    game_id BIGINT NOT NULL COMMENT '游戏ID',
    event_type VARCHAR(30) NOT NULL COMMENT '事件类型',
    player_id BIGINT DEFAULT NULL COMMENT '相关玩家ID',
    data JSON DEFAULT NULL COMMENT '事件详情(JSON)',
    game_time DOUBLE NOT NULL DEFAULT 0 COMMENT '游戏内时间点(秒)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id),
    KEY idx_game_id (game_id),
    KEY idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏事件日志表';
