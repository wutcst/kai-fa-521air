package com.snake.service;

import com.snake.entity.RoomEntity;
import com.snake.entity.RoomPlayer;
import com.snake.repository.RoomPlayerRepository;
import com.snake.repository.RoomRepository;
import com.snake.room.Player;
import com.snake.room.Room;
import com.snake.room.RoomManager;
import com.snake.room.RoomStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 快速匹配服务
 * 维护匹配队列，玩家两两配对，自动创建房间
 */
@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    private final RoomManager roomManager;
    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;

    /** 匹配队列：等待中的玩家 */
    private final ConcurrentLinkedQueue<MatchRequest> queue = new ConcurrentLinkedQueue<>();

    /** 已匹配结果：userId → 匹配到的房间ID（供前端轮询查询） */
    private final Map<String, String> matchResults = new ConcurrentHashMap<>();

    public MatchmakingService(RoomManager roomManager,
                              RoomRepository roomRepository,
                              RoomPlayerRepository roomPlayerRepository) {
        this.roomManager = roomManager;
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
    }

    /**
     * 加入匹配队列
     * 如果队列中已有等待玩家，立即配对；否则加入队列等待
     * @param userId 用户ID
     * @param nickname 昵称
     * @return 匹配状态
     */
    public synchronized MatchResult joinQueue(String userId, String nickname) {
        // 防止重复加入
        if (isInQueue(userId)) {
            return MatchResult.waiting("已在匹配队列中");
        }
        // 防止已匹配的重复加入
        if (matchResults.containsKey(userId)) {
            String existingRoomId = matchResults.get(userId);
            return MatchResult.matched(existingRoomId, "已有匹配结果");
        }

        // 检查队列中是否有等待的玩家（跳过自己，找其他对手）
        MatchRequest opponent = queue.poll();
        while (opponent != null && opponent.userId().equals(userId)) {
            // 取出的是自己（极端情况），暂存并继续找下一个
            queue.add(opponent);
            opponent = queue.poll();
        }
        if (opponent != null) {
            // 配对成功！创建 2 人对战房间
            String roomId = createMatchRoom(opponent, new MatchRequest(userId, nickname));
            matchResults.put(opponent.userId(), roomId);
            matchResults.put(userId, roomId);
            log.info("Match found: {} vs {} → room {}", opponent.nickname(), nickname, roomId);
            return MatchResult.matched(roomId, "匹配成功");
        }

        // 无人等待，加入队列（需再次确认不重复）
        if (!isInQueue(userId)) {
            queue.add(new MatchRequest(userId, nickname));
            log.info("Player {} ({}) added to matchmaking queue, queue size: {}", nickname, userId, queue.size());
        }
        return MatchResult.waiting("已加入匹配队列，等待对手...");
    }

    /**
     * 取消匹配
     */
    public void cancelQueue(String userId) {
        queue.removeIf(r -> r.userId().equals(userId));
        matchResults.remove(userId);
        log.info("Player {} left matchmaking queue", userId);
    }

    /**
     * 查询匹配状态（供前端轮询）
     * @return 若已匹配返回 roomId，否则返回 null
     */
    public String checkMatchStatus(String userId) {
        return matchResults.remove(userId);
    }

    /**
     * 检查是否已在队列中
     */
    public boolean isInQueue(String userId) {
        return queue.stream().anyMatch(r -> r.userId().equals(userId));
    }

    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return queue.size();
    }

    // ---- 内部 ----

    /**
     * 为两个匹配的玩家创建对战房间
     */
    private String createMatchRoom(MatchRequest player1, MatchRequest player2) {
        String roomId = "match_" + System.currentTimeMillis();
        String roomName = "快速对战";

        // 1. 在 RoomManager 中创建房间并预注册玩家（无 WebSocket 会话）
        Room room = new Room(roomId);
        room.setName(roomName);
        room.setGameMode("multi");
        room.setMaxPlayers(2);
        room.setGameDuration(300);
        room.setHasPassword(false);
        room.setAllowBots(false);
        room.setStatus(RoomStatus.WAITING);
        room.setHostId(player1.userId());

        Player p1 = new Player(player1.userId(), player1.nickname(), "", 1, true);
        p1.setReady(true);
        room.getPlayers().put(player1.userId(), p1);

        Player p2 = new Player(player2.userId(), player2.nickname(), "", 1, false);
        room.getPlayers().put(player2.userId(), p2);

        roomManager.registerRoom(room);

        // 2. 持久化到数据库
        RoomEntity entity = new RoomEntity();
        entity.setId(roomId);
        entity.setName(roomName);
        entity.setHostId(player1.userId());
        entity.setGameMode("multi");
        entity.setMaxPlayers(2);
        entity.setGameDuration(300);
        entity.setHasPassword(false);
        entity.setStatus("waiting");
        roomRepository.save(entity);

        // 3. 持久化房间玩家关联
        RoomPlayer rp1 = new RoomPlayer();
        rp1.setRoomId(roomId);
        rp1.setUserId(player1.userId());
        rp1.setHost(true);
        rp1.setReady(true);
        roomPlayerRepository.save(rp1);

        RoomPlayer rp2 = new RoomPlayer();
        rp2.setRoomId(roomId);
        rp2.setUserId(player2.userId());
        rp2.setHost(false);
        rp2.setReady(false);
        roomPlayerRepository.save(rp2);

        log.info("Match room created: id={}, {} vs {}", roomId, player1.nickname(), player2.nickname());
        return roomId;
    }

    // ---- 内部类型 ----

    /** 匹配请求 */
    private record MatchRequest(String userId, String nickname) {}

    /** 匹配结果 */
    public record MatchResult(boolean matched, String roomId, String message) {
        public static MatchResult waiting(String message) {
            return new MatchResult(false, null, message);
        }
        public static MatchResult matched(String roomId, String message) {
            return new MatchResult(true, roomId, message);
        }
    }
}
