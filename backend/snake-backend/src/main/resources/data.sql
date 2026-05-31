-- =============================================
-- 贪吃蛇大作战 - 初始数据脚本
-- 密码使用 BCrypt 加密
-- admin/123456 → $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- =============================================

-- 默认管理员账户 (密码: 123456)
-- 仅在表为空时插入
INSERT IGNORE INTO sys_user (id, username, password, nickname, level, total_score, total_games, wins)
VALUES (1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 10, 9999, 0, 0);

INSERT IGNORE INTO sys_user (id, username, password, nickname, level, total_score, total_games, wins)
VALUES (2, 'player1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '玩家一号', 5, 3500, 0, 0);

INSERT IGNORE INTO sys_user (id, username, password, nickname, level, total_score, total_games, wins)
VALUES (3, 'test', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '测试用户', 1, 100, 0, 0);
