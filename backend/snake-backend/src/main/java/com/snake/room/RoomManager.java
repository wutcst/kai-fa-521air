package com.snake.room;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snake.entity.RoomEntity;
import com.snake.game.GameEngine;
import com.snake.game.GameEngine.GameEvent;
import com.snake.game.GameEngine.GameMode;
import com.snake.game.GameEngine.GameResult;
import com.snake.game.GameEngine.GameStateSnapshot;
import com.snake.game.GameEngine.PlayerSeed;
import com.snake.game.GameEngine.ScoreEntry;
import com.snake.game.GameEngine.SnakeState;
import com.snake.repository.RoomRepository;
import com.snake.service.GameService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.util.WeakHashMap;
import jakarta.annotation.PreDestroy;

@Service
public class RoomManager {
    private static final Logger log = LoggerFactory.getLogger(RoomManager.class);
    private static final int COUNTDOWN_SECONDS = 3;
    private static final List<String> BOT_NAMES = List.of(
        "Swift", "Chaser", "Viper", "Shadow", "Blaze", "Nova", "Echo", "Rogue"
    );

    private final ObjectMapper objectMapper;
    private final GameService gameService;
    private final RoomRepository roomRepository;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, SessionRef> sessionIndex = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    /** 每个 WebSocket Session 的锁对象，防止并发写导致 TEXT_PARTIAL_WRITING */
    private final Map<WebSocketSession, Object> sessionLocks = new WeakHashMap<>();

    public RoomManager(ObjectMapper objectMapper, GameService gameService, RoomRepository roomRepository) {
        this.objectMapper = objectMapper;
        this.gameService = gameService;
        this.roomRepository = roomRepository;
    }

    public void joinRoom(WebSocketSession session, JoinRoomRequest request) {
        if (request == null) {
            sendError(session, "invalid request");
            return;
        }

        String playerId = fallback(request.playerId(), "player_" + session.getId());
        String roomId = fallback(request.roomId(), "room_" + System.currentTimeMillis());

        // ===== 关键修复：加入新房间前，先从所有旧房间中移除该玩家 =====
        // 防止玩家同时存在于多个房间（如上局残留）
        removePlayerFromAllRoomsExcept(playerId, roomId, session);

        Room room = rooms.computeIfAbsent(roomId, id -> createRoom(id, request));
        synchronized (room.getLock()) {
            if (room.getStatus() == RoomStatus.PLAYING && !room.getPlayers().containsKey(playerId)) {
                sendError(session, "room is playing");
                return;
            }
            if (room.isHasPassword() && room.getPassword() != null
                && !Objects.equals(room.getPassword(), request.password())) {
                sendError(session, "invalid password");
                return;
            }

            if (!room.getPlayers().containsKey(playerId)) {
                if (room.getPlayers().size() >= room.getMaxPlayers()) {
                    // ===== 关键修复：检查是否为房主用不同 playerId 重连 =====
                    // 场景：REST 创建房间时用 JWT 用户 ID 注册了房主，
                    // 但前端 getPlayerId() 因 userInfo 丢失回退到了错误 ID
                    if (request.create() && room.getHostId() != null) {
                        Player existingHost = room.getPlayers().get(room.getHostId());
                        if (existingHost != null && existingHost.getSession() == null) {
                            // 确认是房主重连：将当前 session 绑定到房主的真实 playerId
                            log.info("Host reconnecting: session bound to hostId={} (request playerId={}) in room {}",
                                    room.getHostId(), playerId, roomId);
                            existingHost.setSession(session);
                            existingHost.setNickname(safeName(request.nickname()));
                            sessionIndex.put(session.getId(), new SessionRef(roomId, room.getHostId()));
                            broadcastRoomUpdate(room);
                            broadcastSystem(room, existingHost.getNickname() + " joined the room");
                            return;
                        }
                    }
                    sendError(session, "room is full");
                    return;
                }
                boolean isHost = room.getHostId() == null || request.create();
                Player player = new Player(playerId, safeName(request.nickname()), request.avatar(), request.level(), isHost);
                player.setSession(session);
                room.getPlayers().put(playerId, player);
                if (isHost) {
                    room.setHostId(playerId);
                    for (Player p : room.getPlayers().values()) {
                        p.setHost(Objects.equals(p.getId(), playerId));
                    }
                }
                broadcastSystem(room, player.getNickname() + " joined the room");
            } else {
                Player player = room.getPlayers().get(playerId);
                player.setSession(session);
            }
        }

        sessionIndex.put(session.getId(), new SessionRef(roomId, playerId));
        broadcastRoomUpdate(room);
    }

    public void leaveRoom(WebSocketSession session, LeaveRoomRequest request) {
        // ===== 关键修复：使用 request.playerId 作为回退，而非生成临时 ID =====
        String reqRoomId = request != null ? request.roomId() : null;
        String reqPlayerId = request != null ? request.playerId() : null;
        SessionRef ref = sessionIndex.get(session.getId());
        if (ref == null && reqRoomId != null) {
            // sessionIndex 中找不到该 session 时，使用请求中的 roomId + playerId
            // 而不是生成一个不匹配的临时 playerId
            ref = new SessionRef(reqRoomId, fallback(reqPlayerId, "player_" + session.getId()));
        }
        if (ref == null) {
            return;
        }
        Room room = rooms.get(ref.roomId());
        if (room == null) {
            return;
        }

        Player removed;
        synchronized (room.getLock()) {
            removed = room.getPlayers().remove(ref.playerId());
            if (removed != null && room.getEngine() != null) {
                room.getEngine().eliminatePlayer(removed.getId(), "left room");
            }
            if (Objects.equals(room.getHostId(), ref.playerId())) {
                room.setHostId(room.getPlayers().values().stream().findFirst().map(Player::getId).orElse(null));
                for (Player player : room.getPlayers().values()) {
                    player.setHost(Objects.equals(player.getId(), room.getHostId()));
                }
            }
        }

        if (removed != null) {
            broadcastSystem(room, removed.getNickname() + " left the room");
        }
        broadcastRoomUpdate(room);
        cleanupIfEmpty(room);
    }

    public void setReady(WebSocketSession session, ReadyRequest request) {
        SessionRef ref = getSessionRef(session, request == null ? null : request.roomId());
        if (ref == null) {
            return;
        }
        Room room = rooms.get(ref.roomId());
        if (room == null) {
            return;
        }

        synchronized (room.getLock()) {
            Player actor = room.getPlayers().get(ref.playerId());
            if (actor == null) {
                return;
            }

            String targetId = request != null && request.targetPlayerId() != null ? request.targetPlayerId() : actor.getId();
            if (!Objects.equals(targetId, actor.getId()) && !actor.isHost()) {
                sendError(session, "only host can change others");
                return;
            }

            Player target = room.getPlayers().get(targetId);
            if (target == null) {
                return;
            }

            boolean nextReady = request != null && request.ready() != null
                ? request.ready()
                : !target.isReady();
            target.setReady(nextReady);
            broadcastSystem(room, target.getNickname() + (nextReady ? " is ready" : " is not ready"));
        }

        broadcastRoomUpdate(room);
    }

    public void kickPlayer(WebSocketSession session, KickRequest request) {
        SessionRef ref = getSessionRef(session, request == null ? null : request.roomId());
        if (ref == null || request == null || request.targetPlayerId() == null) {
            return;
        }
        Room room = rooms.get(ref.roomId());
        if (room == null) {
            return;
        }

        synchronized (room.getLock()) {
            Player actor = room.getPlayers().get(ref.playerId());
            if (actor == null || !actor.isHost()) {
                sendError(session, "only host can kick");
                return;
            }
            Player target = room.getPlayers().remove(request.targetPlayerId());
            if (target == null) {
                return;
            }
            if (room.getEngine() != null) {
                room.getEngine().eliminatePlayer(target.getId(), "kicked");
            }
            broadcastSystem(room, target.getNickname() + " was kicked");
        }

        broadcastRoomUpdate(room);
        cleanupIfEmpty(room);
    }

    public void startGame(WebSocketSession session, StartGameRequest request) {
        SessionRef ref = getSessionRef(session, request == null ? null : request.roomId());
        if (ref == null) {
            return;
        }
        Room room = rooms.get(ref.roomId());
        if (room == null) {
            return;
        }

        synchronized (room.getLock()) {
            Player actor = room.getPlayers().get(ref.playerId());
            if (actor == null || !actor.isHost()) {
                sendError(session, "only host can start");
                return;
            }
            if (room.getStatus() != RoomStatus.WAITING) {
                sendError(session, "room not ready");
                return;
            }
            if (GameMode.from(room.getGameMode()) == GameMode.MULTI) {
                // AI陪练模式：允许只有房主一人时开始（机器人自动补充）
                if (!room.isAllowBots()) {
                    if (room.getPlayers().size() < 2) {
                        sendError(session, "need at least 2 players");
                        return;
                    }
                    boolean allReady = room.getPlayers().values().stream().allMatch(Player::isReady);
                    if (!allReady) {
                        sendError(session, "not all players ready");
                        return;
                    }
                }
            }
            startCountdown(room);
        }
    }

    public void changeDirection(WebSocketSession session, DirectionRequest request) {
        SessionRef ref = getSessionRef(session, request == null ? null : request.roomId());
        if (ref == null || request == null) {
            return;
        }
        Room room = rooms.get(ref.roomId());
        if (room == null || room.getEngine() == null) {
            return;
        }
        room.getEngine().setDirection(ref.playerId(), GameEngine.Direction.from(request.direction()));
    }

    public void chat(WebSocketSession session, ChatRequest request) {
        SessionRef ref = getSessionRef(session, request == null ? null : request.roomId());
        if (ref == null || request == null || request.text() == null) {
            return;
        }
        Room room = rooms.get(ref.roomId());
        if (room == null) {
            return;
        }
        Player sender = room.getPlayers().get(ref.playerId());
        if (sender == null) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "user");
        payload.put("senderId", sender.getId());
        payload.put("senderName", sender.getNickname());
        payload.put("text", request.text());
        payload.put("time", System.currentTimeMillis());
        broadcast(room, "chat_broadcast", payload);
    }

    public void handleDisconnect(WebSocketSession session) {
        // 清理 session 级别的写锁
        synchronized (sessionLocks) {
            sessionLocks.remove(session);
        }
        SessionRef ref = sessionIndex.remove(session.getId());
        if (ref == null) {
            return;
        }
        Room room = rooms.get(ref.roomId());
        if (room == null) {
            return;
        }

        Player removed;
        synchronized (room.getLock()) {
            removed = room.getPlayers().remove(ref.playerId());
            if (removed != null && room.getEngine() != null) {
                room.getEngine().eliminatePlayer(removed.getId(), "disconnected");
            }
            if (Objects.equals(room.getHostId(), ref.playerId())) {
                room.setHostId(room.getPlayers().values().stream().findFirst().map(Player::getId).orElse(null));
                for (Player player : room.getPlayers().values()) {
                    player.setHost(Objects.equals(player.getId(), room.getHostId()));
                }
            }
        }

        if (removed != null) {
            broadcastSystem(room, removed.getNickname() + " disconnected");
            broadcastRoomUpdate(room);
        }
        cleanupIfEmpty(room);
    }

    public void sendMessage(WebSocketSession session, String type, Object data) {
        send(session, type, data);
    }

    /**
     * 将已构造好的房间注册到 RoomManager（供 MatchmakingService 等外部服务使用）
     * 房间内的玩家此时没有 WebSocket 会话，后续通过 join_room 绑定
     */
    public void registerRoom(Room room) {
        // 清理所有玩家在旧房间中的残留（防止快速匹配玩家同时存在于多个房间）
        for (String playerId : room.getPlayers().keySet()) {
            removePlayerFromAllRoomsExcept(playerId, room.getId(), null);
        }
        rooms.put(room.getId(), room);
        log.info("Room registered in RoomManager: id={}, name={}, players={}",
                room.getId(), room.getName(), room.getPlayers().size());
    }

    public int getOnlineCount() {
        return sessionIndex.size();
    }

    public RoomListResponse listRooms(RoomQuery query) {
        int page = query != null && query.page() > 0 ? query.page() : 1;
        int size = query != null && query.size() > 0 ? query.size() : 10;
        String statusFilter = normalize(query == null ? null : query.status());
        String keyword = query == null ? null : query.keyword();
        String modeFilter = normalize(query == null ? null : query.mode());

        List<RoomSummary> all = new ArrayList<>();
        for (Room room : rooms.values()) {
            all.add(toSummary(room));
        }

        List<RoomSummary> filtered = new ArrayList<>();
        for (RoomSummary summary : all) {
            if (statusFilter != null && !"all".equals(statusFilter)) {
                if (!statusFilter.equals(summary.status())) {
                    continue;
                }
            }
            if (modeFilter != null && !"all".equals(modeFilter)) {
                if (summary.gameMode() == null || !modeFilter.equalsIgnoreCase(summary.gameMode())) {
                    continue;
                }
            }
            if (keyword != null && !keyword.isBlank()) {
                if (summary.name() == null || !summary.name().contains(keyword)) {
                    continue;
                }
            }
            filtered.add(summary);
        }

        int total = filtered.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<RoomSummary> pageList = from >= total ? List.of() : filtered.subList(from, to);

        return new RoomListResponse(pageList, total, page, size);
    }

    /**
     * 获取房间对象（包含密码等完整信息）
     */
    public Room getRoomById(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * 通过 REST API 创建房间时，向 RoomManager 注册房间，
     * 使其出现在实时房间列表中，并允许后续 WebSocket 连接加入。
     */
    public void registerRoom(String roomId, String name, String hostId, String nickname,
                              String gameMode, int maxPlayers, int gameDuration,
                              boolean hasPassword, String password, boolean allowBots) {
        // ===== 关键修复：创建新房间前，先从旧房间中移除房主 =====
        // 防止房主同时存在于多个房间（上局残留导致旧房间玩家出现在新房间）
        removePlayerFromAllRoomsExcept(hostId, roomId, null);

        Room room = new Room(roomId);
        room.setName(name);
        room.setHostId(hostId);
        room.setGameMode(gameMode);
        room.setMaxPlayers(maxPlayers);
        room.setGameDuration(gameDuration);
        room.setHasPassword(hasPassword);
        room.setPassword(password);
        room.setAllowBots(allowBots);
        room.setStatus(RoomStatus.WAITING);

        Player host = new Player(hostId, nickname, "", 1, true);
        room.getPlayers().put(hostId, host);

        rooms.put(roomId, room);
        log.info("Room registered in RoomManager via REST: {} by {}", roomId, nickname);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void startCountdown(Room room) {
        if (room.getCountdownTask() != null) {
            return;
        }
        room.setStatus(RoomStatus.COUNTDOWN);
        room.setCountdownSeconds(COUNTDOWN_SECONDS);
        broadcast(room, "countdown", Map.of("seconds", COUNTDOWN_SECONDS));
        broadcastRoomUpdate(room);

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            synchronized (room.getLock()) {
                int next = room.getCountdownSeconds() - 1;
                room.setCountdownSeconds(next);
                if (next <= 0) {
                    ScheduledFuture<?> current = room.getCountdownTask();
                    if (current != null) {
                        current.cancel(false);
                    }
                    room.setCountdownTask(null);
                    beginGame(room);
                } else {
                    broadcast(room, "countdown", Map.of("seconds", next));
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
        room.setCountdownTask(task);
    }

    private void beginGame(Room room) {
        room.setStatus(RoomStatus.PLAYING);
        broadcastRoomUpdate(room);
        List<PlayerSeed> seeds = new ArrayList<>();
        room.getPlayers().values().forEach(p -> seeds.add(new PlayerSeed(p.getId(), p.getNickname(), false)));

        if (room.isAllowBots() && room.getPlayers().size() < room.getMaxPlayers()) {
            int botsToAdd = room.getMaxPlayers() - room.getPlayers().size();
            for (int i = 0; i < botsToAdd; i++) {
                String botId = "bot_" + (i + 1) + "_" + room.getId();
                String name = BOT_NAMES.get(i % BOT_NAMES.size());
                seeds.add(new PlayerSeed(botId, name, true));
            }
        }

        log.info("Starting game engine for room {} with {} players (mode={})", room.getId(), seeds.size(), room.getGameMode());
        GameEngine engine = new GameEngine(GameMode.from(room.getGameMode()), room.getGameDuration(), seeds);
        room.setEngine(engine);
        room.setStartedAt(LocalDateTime.now());

        engine.start(
            scheduler,
            state -> broadcastGameState(room, state),
            event -> broadcastEvent(room, event),
            result -> handleGameOver(room, result)
        );

        log.info("Game engine started for room {}", room.getId());
        broadcast(room, "game_start", Map.of("roomId", room.getId(), "gameMode", room.getGameMode()));
    }

    private void handleGameOver(Room room, GameResult result) {
        log.info("handleGameOver: room={}, mode={}, rankings={}", room.getId(), result.gameMode(), result.rankings().size());
        room.setStatus(RoomStatus.FINISHED);
        broadcastRoomUpdate(room);

        // 同步更新数据库中房间状态为 finished
        try {
            roomRepository.findById(room.getId()).ifPresent(r -> {
                r.setStatus("finished");
                roomRepository.save(r);
            });
            log.info("Room status updated to finished in DB: {}", room.getId());
        } catch (Exception e) {
            log.error("Failed to update room status in DB for room {}: {}", room.getId(), e.getMessage());
        }

        // 持久化游戏结果到数据库，获取真实的数据库 gameId
        Long dbGameId = null;
        try {
            com.snake.entity.GameEntity saved = gameService.saveGameResult(room, result);
            dbGameId = saved.getId();
            log.info("Game result saved to DB: gameId={}, room={}", dbGameId, room.getId());
        } catch (Exception e) {
            log.error("Failed to persist game result for room {}: {}", room.getId(), e.getMessage(), e);
        }

        // 使用真实的数据库 ID 发送 game_over 给每个玩家
        final String realGameId = dbGameId != null ? String.valueOf(dbGameId) : result.gameId();
        for (Player player : room.getPlayers().values()) {
            GameResult decorated = decorateResultForPlayer(result, player.getId());
            GameResult withRealId = new GameResult(realGameId, decorated.duration(), decorated.rankings(), decorated.gameMode());
            send(player.getSession(), "game_over", withRealId);
        }
    }

    private void broadcastGameState(Room room, GameStateSnapshot snapshot) {
        for (Player player : room.getPlayers().values()) {
            GameStateSnapshot decorated = decorateStateForPlayer(snapshot, player.getId());
            send(player.getSession(), "game_state", decorated);
        }
    }

    private void broadcastEvent(Room room, GameEvent event) {
        broadcast(room, event.type(), event.data());
    }

    private GameStateSnapshot decorateStateForPlayer(GameStateSnapshot snapshot, String playerId) {
        Map<String, SnakeState> snakes = new LinkedHashMap<>();
        for (Map.Entry<String, SnakeState> entry : snapshot.snakes().entrySet()) {
            SnakeState s = entry.getValue();
            snakes.put(entry.getKey(), new SnakeState(
                s.id(),
                s.body(),
                s.direction(),
                s.color(),
                s.score(),
                s.kills(),
                s.length(),
                s.isAlive(),
                s.nickname(),
                s.speedBoost(),
                s.shield(),
                s.magnet(),
                Objects.equals(entry.getKey(), playerId)
            ));
        }

        List<ScoreEntry> scoreBoard = snapshot.scoreBoard().stream()
            .map(entry -> new ScoreEntry(
                entry.id(),
                entry.nickname(),
                entry.score(),
                entry.kills(),
                entry.length(),
                entry.isAlive(),
                entry.survivalTime(),
                Objects.equals(entry.id(), playerId),
                entry.color()
            ))
            .toList();

        return new GameStateSnapshot(
            snakes,
            snapshot.foods(),
            snapshot.items(),
            snapshot.obstacles(),
            scoreBoard,
            snapshot.gameTime(),
            snapshot.totalTime(),
            snapshot.gameStatus(),
            snapshot.mapWidth(),
            snapshot.mapHeight(),
            snapshot.gridSize()
        );
    }

    private GameResult decorateResultForPlayer(GameResult result, String playerId) {
        List<ScoreEntry> decorated = result.rankings().stream()
            .map(entry -> new ScoreEntry(
                entry.id(),
                entry.nickname(),
                entry.score(),
                entry.kills(),
                entry.length(),
                entry.isAlive(),
                entry.survivalTime(),
                Objects.equals(entry.id(), playerId),
                entry.color()
            ))
            .toList();
        return new GameResult(result.gameId(), result.duration(), decorated, result.gameMode());
    }

    private void broadcastRoomUpdate(Room room) {
        Map<String, Object> roomInfo = new LinkedHashMap<>();
        roomInfo.put("id", room.getId());
        roomInfo.put("name", room.getName());
        roomInfo.put("hostId", room.getHostId());
        roomInfo.put("playerCount", room.getPlayers().size());
        roomInfo.put("maxPlayers", room.getMaxPlayers());
        roomInfo.put("status", room.getStatus().toWire());
        roomInfo.put("hasPassword", room.isHasPassword());
        roomInfo.put("gameDuration", room.getGameDuration());
        roomInfo.put("gameMode", room.getGameMode());
        roomInfo.put("allowBots", room.isAllowBots());

        List<Map<String, Object>> players = room.getPlayers().values().stream()
            .map(player -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("id", player.getId());
                data.put("nickname", player.getNickname());
                data.put("avatar", player.getAvatar());
                data.put("level", player.getLevel());
                data.put("isHost", player.isHost());
                data.put("isReady", player.isReady());
                return data;
            })
            .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("roomId", room.getId());
        payload.put("roomInfo", roomInfo);
        payload.put("players", players);
        broadcast(room, "room_update", payload);
    }

    private void broadcastSystem(Room room, String text) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "system");
        payload.put("text", text);
        payload.put("time", System.currentTimeMillis());
        broadcast(room, "chat_broadcast", payload);
    }

    private void broadcast(Room room, String type, Object data) {
        for (Player player : room.getPlayers().values()) {
            send(player.getSession(), type, data);
        }
    }

    private boolean send(WebSocketSession session, String type, Object data) {
        if (session == null || !session.isOpen()) {
            if (session == null) {
                log.warn("WS send skipped: session is null for type={}", type);
            }
            return false;
        }
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("type", type);
        wrapper.put("data", data);
        // 同步 session 级别的锁，防止多个线程同时往同一个 session 写消息
        Object lock;
        synchronized (sessionLocks) {
            lock = sessionLocks.computeIfAbsent(session, k -> new Object());
        }
        synchronized (lock) {
            try {
                String json = objectMapper.writeValueAsString(wrapper);
                session.sendMessage(new TextMessage(json));
                return true;
            } catch (JsonProcessingException e) {
                log.error("WS JSON serialization error for type={}: {}", type, e.getMessage());
                return false;
            } catch (Exception e) {
                log.error("WS send error for type={}: {}", type, e.getMessage());
                return false;
            }
        }
    }

    private void sendError(WebSocketSession session, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", message);
        send(session, "error", payload);
    }

    private Room createRoom(String roomId, JoinRoomRequest request) {
        Room room = new Room(roomId);
        room.setName(fallback(request.roomName(), "Room"));
        room.setGameMode(fallback(request.gameMode(), "multi"));
        int maxPlayers = request.maxPlayers() > 0 ? request.maxPlayers() : 6;
        if (GameMode.from(room.getGameMode()) == GameMode.SINGLE) {
            maxPlayers = 1;
        }
        room.setMaxPlayers(maxPlayers);
        int duration = request.gameDuration() > 0 ? request.gameDuration() : 300;
        if (GameMode.from(room.getGameMode()) == GameMode.SINGLE) {
            duration = 0;
        }
        room.setGameDuration(duration);
        room.setHasPassword(request.hasPassword());
        room.setPassword(request.password());
        room.setAllowBots(request.allowBots());
        room.setStatus(RoomStatus.WAITING);
        return room;
    }

    private SessionRef getSessionRef(WebSocketSession session, String roomId) {
        SessionRef ref = sessionIndex.get(session.getId());
        if (ref == null && roomId != null) {
            return new SessionRef(roomId, "player_" + session.getId());
        }
        return ref;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeName(String nickname) {
        String value = nickname == null || nickname.isBlank() ? "Player" : nickname;
        return value.length() > 20 ? value.substring(0, 20) : value;
    }

    private void cleanupIfEmpty(Room room) {
        if (room.getPlayers().isEmpty()) {
            // 如果房间已经结束或被清理，确保数据库状态同步为 finished
            if (room.getStatus() == RoomStatus.FINISHED || room.getStatus() == RoomStatus.PLAYING) {
                try {
                    roomRepository.findById(room.getId()).ifPresent(r -> {
                        if (!"finished".equals(r.getStatus())) {
                            r.setStatus("finished");
                            roomRepository.save(r);
                            log.info("Room {} DB status synced to finished during cleanup", room.getId());
                        }
                    });
                } catch (Exception e) {
                    log.error("Failed to sync room status during cleanup for {}: {}", room.getId(), e.getMessage());
                }
            }
            rooms.remove(room.getId());
            if (room.getEngine() != null) {
                room.getEngine().stop();
            }
        }
    }

    /**
     * 从所有房间中移除指定玩家（除了 excludeRoomId 指定的房间）。
     * 用于玩家切换房间时，确保不会同时存在于多个房间。
     * 如果旧房间变空，会自动清理。
     */
    private void removePlayerFromAllRoomsExcept(String playerId, String excludeRoomId, WebSocketSession newSession) {
        List<Room> affectedRooms = new ArrayList<>();
        for (Room r : rooms.values()) {
            if (r.getId().equals(excludeRoomId)) {
                continue;
            }
            Player removed;
            synchronized (r.getLock()) {
                removed = r.getPlayers().remove(playerId);
            }
            if (removed != null) {
                log.info("Player {} removed from old room {} (joining new room {})",
                        playerId, r.getId(), excludeRoomId);
                // 如果该玩家正在游戏中（引擎存在），消除他
                if (r.getEngine() != null) {
                    r.getEngine().eliminatePlayer(playerId, "switched room");
                }
                // 如果移除的是房主，转移房主
                if (Objects.equals(r.getHostId(), playerId)) {
                    r.setHostId(r.getPlayers().values().stream()
                            .findFirst().map(Player::getId).orElse(null));
                    for (Player p : r.getPlayers().values()) {
                        p.setHost(Objects.equals(p.getId(), r.getHostId()));
                    }
                }
                broadcastSystem(r, removed.getNickname() + " left the room");
                affectedRooms.add(r);
            }
        }
        // 广播受影响房间的更新，并清理空房间
        for (Room r : affectedRooms) {
            broadcastRoomUpdate(r);
            cleanupIfEmpty(r);
        }
    }

    private RoomSummary toSummary(Room room) {
        int playerCount = room.getPlayers().size();
        String status = computeStatus(room, playerCount);
        String hostName = null;
        if (room.getHostId() != null) {
            Player host = room.getPlayers().get(room.getHostId());
            if (host != null) {
                hostName = host.getNickname();
            }
        }
        return new RoomSummary(
            room.getId(),
            room.getName(),
            room.getHostId(),
            hostName,
            playerCount,
            room.getMaxPlayers(),
            status,
            room.isHasPassword(),
            room.getGameDuration(),
            room.getGameMode(),
            room.isAllowBots()
        );
    }

    private String computeStatus(Room room, int playerCount) {
        String status = switch (room.getStatus()) {
            case PLAYING -> "playing";
            case FINISHED -> "finished";
            case COUNTDOWN -> "waiting";
            case WAITING -> "waiting";
        };
        if (!"playing".equals(status) && !"finished".equals(status) && playerCount >= room.getMaxPlayers()) {
            return "full";
        }
        return status;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private record SessionRef(String roomId, String playerId) {
    }

    public record JoinRoomRequest(
        String roomId,
        String roomName,
        String playerId,
        String nickname,
        String avatar,
        int level,
        String gameMode,
        int maxPlayers,
        int gameDuration,
        boolean hasPassword,
        String password,
        boolean create,
        boolean allowBots
    ) {
    }

    public record LeaveRoomRequest(String roomId, String playerId) {
    }

    public record ReadyRequest(String roomId, String playerId, String targetPlayerId, Boolean ready) {
    }

    public record StartGameRequest(String roomId, String playerId) {
    }

    public record DirectionRequest(String roomId, String playerId, String direction) {
    }

    public record ItemRequest(String roomId, String playerId, String itemType) {
    }

    public record SpeedBoostRequest(String roomId, String playerId) {
    }

    public record ChatRequest(String roomId, String playerId, String text) {
    }

    public record KickRequest(String roomId, String playerId, String targetPlayerId) {
    }

    public record RoomSummary(
        String id,
        String name,
        String hostId,
        String hostName,
        int playerCount,
        int maxPlayers,
        String status,
        boolean hasPassword,
        int gameDuration,
        String gameMode,
        boolean allowBots
    ) {
    }

    public record RoomListResponse(List<RoomSummary> list, int total, int page, int size) {
    }

    public record RoomQuery(int page, int size, String status, String keyword, String mode) {
    }
}
