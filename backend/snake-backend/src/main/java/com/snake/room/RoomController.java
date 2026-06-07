package com.snake.room;

import com.snake.dto.CreateRoomRequest;
import com.snake.dto.JoinRoomRequest;
import com.snake.dto.RoomResponse;
import com.snake.dto.RoomResponse.PlayerInfo;
import com.snake.entity.RoomEntity;
import com.snake.entity.RoomPlayer;
import com.snake.entity.SysUser;
import com.snake.repository.RoomPlayerRepository;
import com.snake.repository.RoomRepository;
import com.snake.repository.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 房间 REST API
 * 提供房间列表、创建、加入、退出、更新等接口
 * 同时将房间数据持久化到数据库，并与 RoomManager (WebSocket) 同步
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    private final RoomManager roomManager;
    private final SysUserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;

    public RoomController(RoomManager roomManager, SysUserRepository userRepository,
                          RoomRepository roomRepository, RoomPlayerRepository roomPlayerRepository) {
        this.roomManager = roomManager;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
    }

    /**
     * 获取房间列表
     * 从数据库获取持久化房间，并与 RoomManager 中的实时房间合并
     */
    @GetMapping("/list")
    public RoomManager.RoomListResponse listRooms(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "100") int size,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "mode", required = false) String mode) {

        // 从数据库查询房间（默认排除已结束的房间）
        List<RoomEntity> dbRooms;
        if (status != null && !status.isBlank() && !"all".equals(status)) {
            dbRooms = roomRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            // 大厅列表不展示已结束（finished）的房间
            dbRooms = roomRepository.findAll().stream()
                    .filter(r -> !"finished".equals(r.getStatus()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
        }

        // 过滤关键字和模式
        List<RoomEntity> filtered = dbRooms.stream()
                .filter(r -> keyword == null || keyword.isBlank() || r.getName().contains(keyword))
                .filter(r -> mode == null || mode.isBlank() || "all".equals(mode) || mode.equalsIgnoreCase(r.getGameMode()))
                .collect(Collectors.toList());

        // 合并 RoomManager 中的实时房间（WebSocket 创建但尚未持久化的）
        Map<String, RoomManager.RoomSummary> liveRoomMap = new LinkedHashMap<>();
        RoomManager.RoomListResponse liveResponse = roomManager.listRooms(
                new RoomManager.RoomQuery(1, 1000, null, null, null));
        for (RoomManager.RoomSummary s : liveResponse.list()) {
            liveRoomMap.put(s.id(), s);
        }

        // 将 DB 房间转换为 RoomSummary，优先使用实时数据（playerCount 等）
        List<RoomManager.RoomSummary> merged = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (RoomEntity r : filtered) {
            seenIds.add(r.getId());
            RoomManager.RoomSummary live = liveRoomMap.get(r.getId());
            if (live != null) {
                // 使用实时数据（玩家数量更准确）
                merged.add(live);
            } else {
                // DB 中有记录但 RoomManager 中没有的房间：
                // 如果状态是 waiting 且不在实时列表中，说明已被清理但 DB 未更新，跳过
                if ("waiting".equals(r.getStatus())) {
                    log.info("Skipping stale room {} from DB (status=waiting, not in live RoomManager)", r.getId());
                    continue;
                }
                // 使用 DB 数据（finished 状态的房间保留以展示历史）
                long playerCount = roomPlayerRepository.countByRoomId(r.getId());
                // 查房主昵称
                String hostName = null;
                if (r.getHostId() != null) {
                    hostName = userRepository.findById(Long.valueOf(r.getHostId()))
                            .map(SysUser::getNickname).orElse(null);
                }
                merged.add(new RoomManager.RoomSummary(
                        r.getId(), r.getName(), r.getHostId(), hostName,
                        (int) playerCount, r.getMaxPlayers(), r.getStatus(),
                        r.isHasPassword(), r.getGameDuration(), r.getGameMode()
                ));
            }
        }

        // 添加仅在 RoomManager 中存在（WebSocket 创建）的房间
        for (RoomManager.RoomSummary live : liveRoomMap.values()) {
            if (!seenIds.contains(live.id())) {
                merged.add(live);
            }
        }

        // 排序：waiting 状态优先，按创建时间倒序
        merged.sort((a, b) -> {
            int aOrder = "waiting".equals(a.status()) || "full".equals(a.status()) ? 0 : 1;
            int bOrder = "waiting".equals(b.status()) || "full".equals(b.status()) ? 0 : 1;
            if (aOrder != bOrder) return aOrder - bOrder;
            return b.id().compareTo(a.id());
        });

        // 分页
        int total = merged.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<RoomManager.RoomSummary> pageList = from >= total ? List.of() : merged.subList(from, to);

        return new RoomManager.RoomListResponse(pageList, total, page, size);
    }

    /**
     * 获取在线人数
     */
    @GetMapping("/online-count")
    public Map<String, Object> onlineCount() {
        return Map.of("count", roomManager.getOnlineCount());
    }

    /**
     * 创建房间
     * 同时持久化到数据库和 RoomManager
     */
    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequest request,
                                         Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        SysUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户不存在"));
        }

        String roomId = "room_" + System.currentTimeMillis() + "_" + userId;
        String userStrId = String.valueOf(userId);
        String roomName = request.getName() != null && !request.getName().isBlank()
                ? request.getName() : user.getNickname() + "的房间";
        String gameMode = request.getGameMode() != null ? request.getGameMode() : "multi";
        boolean isSingle = "single".equals(gameMode);
        int maxPlayers = isSingle ? 1 : (request.getMaxPlayers() > 0 ? request.getMaxPlayers() : 6);
        int gameDuration = isSingle ? 0 : (request.getGameDuration() > 0 ? request.getGameDuration() : 300);

        // 1. 持久化到数据库
        RoomEntity roomEntity = new RoomEntity();
        roomEntity.setId(roomId);
        roomEntity.setName(roomName);
        roomEntity.setHostId(userStrId);
        roomEntity.setGameMode(gameMode);
        roomEntity.setMaxPlayers(maxPlayers);
        roomEntity.setGameDuration(gameDuration);
        roomEntity.setHasPassword(request.isHasPassword());
        roomEntity.setPassword(request.getPassword());
        roomEntity.setAllowBots(request.isAllowBots());
        roomEntity.setStatus("waiting");
        roomRepository.save(roomEntity);

        // 2. 保存房主玩家记录
        RoomPlayer hostPlayer = new RoomPlayer();
        hostPlayer.setRoomId(roomId);
        hostPlayer.setUserId(userStrId);
        hostPlayer.setHost(true);
        hostPlayer.setReady(true);
        roomPlayerRepository.save(hostPlayer);

        // 3. 同步注册到 RoomManager（实时 WebSocket 可见）
        roomManager.registerRoom(roomId, roomName, userStrId, user.getNickname(),
                gameMode, maxPlayers, gameDuration,
                request.isHasPassword(), request.getPassword(), request.isAllowBots());

        log.info("Room created and persisted: {} by user {} (id={})", roomId, user.getUsername(), userId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roomId", roomId);
        data.put("name", roomName);
        data.put("gameMode", gameMode);
        data.put("maxPlayers", maxPlayers);
        data.put("gameDuration", gameDuration);
        data.put("hasPassword", request.isHasPassword());
        data.put("password", request.getPassword());
        data.put("allowBots", request.isAllowBots());
        data.put("hostId", userStrId);
        data.put("hostName", user.getNickname());
        data.put("playerCount", 1);
        data.put("status", "waiting");

        return ResponseEntity.ok(data);
    }

    /**
     * 加入房间
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId,
                                       @RequestBody(required = false) JoinRoomRequest request,
                                       Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        String userStrId = String.valueOf(userId);

        // 从 RoomManager 获取房间信息
        RoomManager.RoomSummary summary = findRoomSummary(roomId);
        if (summary == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "房间不存在"));
        }
        if ("playing".equals(summary.status())) {
            return ResponseEntity.badRequest().body(Map.of("message", "游戏已开始，无法加入"));
        }
        if (summary.playerCount() >= summary.maxPlayers()) {
            return ResponseEntity.badRequest().body(Map.of("message", "房间已满"));
        }
        if (summary.hasPassword()) {
            String pwd = request != null ? request.getPassword() : null;
            if (pwd == null || pwd.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "需要房间密码"));
            }
            // 验证密码：优先从 RoomManager 获取，其次从数据库获取
            Room room = roomManager.getRoomById(roomId);
            String storedPassword = null;
            if (room != null) {
                storedPassword = room.getPassword();
            } else {
                storedPassword = roomRepository.findById(roomId)
                        .map(RoomEntity::getPassword).orElse(null);
            }
            if (storedPassword != null && !storedPassword.equals(pwd)) {
                return ResponseEntity.badRequest().body(Map.of("message", "密码错误"));
            }
        }

        // 持久化玩家加入记录到数据库
        Optional<RoomPlayer> existing = roomPlayerRepository.findByRoomIdAndUserId(roomId, userStrId);
        if (existing.isEmpty()) {
            RoomPlayer player = new RoomPlayer();
            player.setRoomId(roomId);
            player.setUserId(userStrId);
            player.setHost(false);
            player.setReady(false);
            roomPlayerRepository.save(player);
        }

        log.info("Player {} joined room {} via REST", userId, roomId);
        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "success", true
        ));
    }

    /**
     * 退出房间
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomId,
                                        Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        String userStrId = String.valueOf(userId);

        // 从数据库删除玩家记录
        roomPlayerRepository.deleteByRoomIdAndUserId(roomId, userStrId);
        log.info("Player {} left room {} via REST", userId, roomId);

        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 更新房间设置
     */
    @PutMapping("/{roomId}")
    public ResponseEntity<?> updateRoom(@PathVariable String roomId,
                                         @RequestBody Map<String, Object> settings,
                                         Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }

        // 更新数据库记录
        Optional<RoomEntity> optRoom = roomRepository.findById(roomId);
        if (optRoom.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "房间不存在"));
        }

        RoomEntity room = optRoom.get();
        if (settings.containsKey("name")) {
            room.setName((String) settings.get("name"));
        }
        if (settings.containsKey("maxPlayers")) {
            room.setMaxPlayers((Integer) settings.get("maxPlayers"));
        }
        if (settings.containsKey("gameDuration")) {
            room.setGameDuration((Integer) settings.get("gameDuration"));
        }
        if (settings.containsKey("hasPassword")) {
            room.setHasPassword((Boolean) settings.get("hasPassword"));
        }
        if (settings.containsKey("password")) {
            room.setPassword((String) settings.get("password"));
        }
        if (settings.containsKey("allowBots")) {
            room.setAllowBots((Boolean) settings.get("allowBots"));
        }
        roomRepository.save(room);

        log.info("Room settings updated: {} settings={}", roomId, settings);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 获取房间详情
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId) {
        // 优先从 RoomManager 获取实时数据
        RoomManager.RoomSummary summary = findRoomSummary(roomId);
        if (summary != null) {
            RoomResponse resp = new RoomResponse();
            resp.setRoomId(summary.id());
            resp.setName(summary.name());
            resp.setHostId(summary.hostId());
            resp.setHostName(summary.hostName());
            resp.setPlayerCount(summary.playerCount());
            resp.setMaxPlayers(summary.maxPlayers());
            resp.setStatus(summary.status());
            resp.setHasPassword(summary.hasPassword());
            resp.setGameDuration(summary.gameDuration());
            resp.setGameMode(summary.gameMode());
            resp.setPlayers(List.of());
            return ResponseEntity.ok(resp);
        }

        // 从数据库查询
        Optional<RoomEntity> optRoom = roomRepository.findById(roomId);
        if (optRoom.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "房间不存在"));
        }

        RoomEntity room = optRoom.get();
        String hostName = null;
        if (room.getHostId() != null) {
            hostName = userRepository.findById(Long.valueOf(room.getHostId()))
                    .map(SysUser::getNickname).orElse(null);
        }
        long playerCount = roomPlayerRepository.countByRoomId(roomId);

        RoomResponse resp = new RoomResponse();
        resp.setRoomId(room.getId());
        resp.setName(room.getName());
        resp.setHostId(room.getHostId());
        resp.setHostName(hostName);
        resp.setPlayerCount((int) playerCount);
        resp.setMaxPlayers(room.getMaxPlayers());
        resp.setStatus(room.getStatus());
        resp.setHasPassword(room.isHasPassword());
        resp.setGameDuration(room.getGameDuration());
        resp.setGameMode(room.getGameMode());
        resp.setPlayers(List.of());

        return ResponseEntity.ok(resp);
    }

    private RoomManager.RoomSummary findRoomSummary(String roomId) {
        RoomManager.RoomListResponse response = roomManager.listRooms(
                new RoomManager.RoomQuery(1, 1000, null, null, null));
        return response.list().stream()
                .filter(s -> s.id().equals(roomId))
                .findFirst()
                .orElse(null);
    }
}
