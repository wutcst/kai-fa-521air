-- =============================================
-- 贪吃蛇大作战 - 数据库迁移脚本 v2
-- 修复表结构：将旧版 BIGINT 类型的 ID 字段
-- 改为 VARCHAR(64)，与新代码对齐
-- =============================================

-- ----------------------------
-- 1. 修复 game 表：room_id 从 BIGINT -> VARCHAR(64)
-- ----------------------------
ALTER TABLE game
    MODIFY COLUMN room_id VARCHAR(64) DEFAULT NULL COMMENT '所属房间ID';

-- ----------------------------
-- 2. 修复 game_player_result 表：确保 user_id 为 VARCHAR(64)
-- ----------------------------
ALTER TABLE game_player_result
    MODIFY COLUMN user_id VARCHAR(64) NOT NULL COMMENT '用户ID';

-- ----------------------------
-- 3. 修复 room_player 表：room_id 和 user_id
-- ----------------------------
ALTER TABLE room_player
    MODIFY COLUMN room_id VARCHAR(64) NOT NULL COMMENT '房间ID',
    MODIFY COLUMN user_id VARCHAR(64) NOT NULL COMMENT '用户ID';

-- ----------------------------
-- 4. 修复 game_event_log 表：player_id 可为 NULL 的 BIGINT
--    这个字段本身就关联到 game_player_result.id (BIGINT)，不需要改
-- ----------------------------

-- =============================================
-- 验证：检查当前表结构是否已更新
-- =============================================
-- DESC game;
-- DESC game_player_result;
-- DESC room_player;
