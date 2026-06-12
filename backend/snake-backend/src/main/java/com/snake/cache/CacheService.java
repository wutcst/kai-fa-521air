package com.snake.cache;

import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Redis 缓存服务 为贪吃蛇游戏提供在线计数、房间缓存、会话管理等能力 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private static final String KEY_ONLINE_COUNT = "snake:online:count";
    private static final String KEY_SESSION = "snake:session:";
    private static final String KEY_ROOM = "snake:room:";
    private static final String KEY_ROOM_LIST = "snake:room:list";
    private static final String KEY_ROOM_PLAYERS = "snake:room:players:";
    private static final String KEY_RATE_LIMIT = "snake:ratelimit:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public CacheService(
            RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // ==================== Online Count ====================

    public long incrementOnline() {
        Long count = stringRedisTemplate.opsForValue().increment(KEY_ONLINE_COUNT);
        return count != null ? count : 0;
    }

    public long decrementOnline() {
        Long count = stringRedisTemplate.opsForValue().decrement(KEY_ONLINE_COUNT);
        if (count != null && count < 0) {
            stringRedisTemplate.delete(KEY_ONLINE_COUNT);
            return 0;
        }
        return count != null ? count : 0;
    }

    public long getOnlineCount() {
        String val = stringRedisTemplate.opsForValue().get(KEY_ONLINE_COUNT);
        return val != null ? Long.parseLong(val) : 0;
    }

    // ==================== Session Index ====================
    // Maps WS sessionId -> roomId:playerId for multi-instance support

    public void saveSession(String wsSessionId, String roomId, String playerId) {
        stringRedisTemplate
                .opsForValue()
                .set(KEY_SESSION + wsSessionId, roomId + ":" + playerId, 30, TimeUnit.MINUTES);
    }

    public String getSessionRoom(String wsSessionId) {
        String val = stringRedisTemplate.opsForValue().get(KEY_SESSION + wsSessionId);
        if (val == null) return null;
        return val.contains(":") ? val.split(":", 2)[0] : null;
    }

    public String getSessionPlayer(String wsSessionId) {
        String val = stringRedisTemplate.opsForValue().get(KEY_SESSION + wsSessionId);
        if (val == null) return null;
        return val.contains(":") ? val.split(":", 2)[1] : null;
    }

    public void removeSession(String wsSessionId) {
        stringRedisTemplate.delete(KEY_SESSION + wsSessionId);
    }

    // ==================== Room Cache ====================

    public void cacheRoomSummary(String roomId, Map<String, Object> summary) {
        redisTemplate.opsForValue().set(KEY_ROOM + roomId, summary, 1, TimeUnit.HOURS);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRoomSummary(String roomId) {
        Object val = redisTemplate.opsForValue().get(KEY_ROOM + roomId);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }

    public void removeRoomCache(String roomId) {
        redisTemplate.delete(KEY_ROOM + roomId);
        redisTemplate.delete(KEY_ROOM_PLAYERS + roomId);
    }

    // ==================== Room List ====================

    public void addRoomToList(String roomId) {
        stringRedisTemplate.opsForSet().add(KEY_ROOM_LIST, roomId);
    }

    public void removeRoomFromList(String roomId) {
        stringRedisTemplate.opsForSet().remove(KEY_ROOM_LIST, roomId);
    }

    public Set<String> getAllRoomIds() {
        return stringRedisTemplate.opsForSet().members(KEY_ROOM_LIST);
    }

    // ==================== Room Players ====================

    public void addPlayerToRoom(String roomId, String playerId, String playerJson) {
        redisTemplate.opsForHash().put(KEY_ROOM_PLAYERS + roomId, playerId, playerJson);
        stringRedisTemplate.expire(KEY_ROOM_PLAYERS + roomId, 1, TimeUnit.HOURS);
    }

    public void removePlayerFromRoom(String roomId, String playerId) {
        redisTemplate.opsForHash().delete(KEY_ROOM_PLAYERS + roomId, playerId);
    }

    public Map<Object, Object> getRoomPlayers(String roomId) {
        return redisTemplate.opsForHash().entries(KEY_ROOM_PLAYERS + roomId);
    }

    public long getRoomPlayerCount(String roomId) {
        Long count = redisTemplate.opsForHash().size(KEY_ROOM_PLAYERS + roomId);
        return count != null ? count : 0;
    }

    // ==================== Rate Limiting ====================

    public boolean allowAction(String playerId, String action, int maxPerSecond) {
        String key = KEY_RATE_LIMIT + playerId + ":" + action;
        long now = System.currentTimeMillis() / 1000;
        String windowKey = key + ":" + now;

        Long count = stringRedisTemplate.opsForValue().increment(windowKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(windowKey, 2, TimeUnit.SECONDS);
        }
        return count != null && count <= maxPerSecond;
    }

    // ==================== Cache Health ====================

    public boolean ping() {
        try {
            return "PONG".equals(stringRedisTemplate.getConnectionFactory().getConnection().ping());
        } catch (Exception e) {
            log.warn("Redis ping failed: {}", e.getMessage());
            return false;
        }
    }
}
