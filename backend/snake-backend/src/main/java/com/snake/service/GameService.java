package com.snake.service;

import com.snake.entity.GameEntity;
import com.snake.entity.GamePlayerResult;
import com.snake.entity.SysUser;
import com.snake.game.GameEngine.GameResult;
import com.snake.game.GameEngine.ScoreEntry;
import com.snake.repository.GamePlayerResultRepository;
import com.snake.repository.GameRepository;
import com.snake.repository.SysUserRepository;
import com.snake.room.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 游戏结果持久化服务
 * 游戏结束时保存游戏记录和玩家成绩，并提供查询 API
 */
@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;
    private final GamePlayerResultRepository playerResultRepository;
    private final SysUserRepository userRepository;

    public GameService(GameRepository gameRepository,
                       GamePlayerResultRepository playerResultRepository,
                       SysUserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.playerResultRepository = playerResultRepository;
        this.userRepository = userRepository;
    }

    /**
     * 保存游戏结果（事务）：创建游戏记录 + 玩家成绩 + 更新用户统计
     */
    @Transactional
    public GameEntity saveGameResult(Room room, GameResult result) {
        LocalDateTime endedAt = LocalDateTime.now();
        LocalDateTime startedAt = room.getStartedAt() != null ? room.getStartedAt() : endedAt;

        // 1. 创建游戏记录
        GameEntity game = new GameEntity();
        game.setRoomId(room.getId());
        game.setGameMode(room.getGameMode() != null ? room.getGameMode() : result.gameMode());
        game.setDuration(result.duration());
        game.setPlayerCount(result.rankings().size());
        game.setStatus("finished");
        game.setStartedAt(startedAt);
        game.setEndedAt(endedAt);
        game = gameRepository.save(game);

        Long gameId = game.getId();
        log.info("Game saved: id={}, room={}, mode={}, duration={}s, players={}",
                gameId, room.getId(), game.getGameMode(), game.getDuration(), game.getPlayerCount());

        // 2. 保存每位玩家的成绩
        int rank = 1;
        for (ScoreEntry entry : result.rankings()) {
            GamePlayerResult gpr = new GamePlayerResult();
            gpr.setGameId(gameId);
            gpr.setUserId(entry.id());  // player id (String)
            gpr.setScore(entry.score());
            gpr.setKills(entry.kills());
            gpr.setSnakeLength(entry.length());
            gpr.setSurvivalTime(entry.survivalTime());
            gpr.setAlive(entry.isAlive());
            gpr.setRank(rank);
            // 判断是否为 Bot（playerId 以 "bot_" 开头）
            gpr.setBot(entry.id() != null && entry.id().startsWith("bot_"));
            playerResultRepository.save(gpr);

            // 3. 更新用户统计数据（非 Bot 玩家）
            boolean isWinner = (rank == 1);
            if (!entry.id().startsWith("bot_")) {
                try {
                    Long userId = Long.parseLong(entry.id());
                    userRepository.findById(userId).ifPresent(user -> {
                        user.setTotalGames(user.getTotalGames() + 1);
                        user.setTotalScore(user.getTotalScore() + entry.score());
                        if (isWinner) {
                            user.setWins(user.getWins() + 1);
                        }
                        userRepository.save(user);
                    });
                } catch (NumberFormatException e) {
                    log.warn("Cannot update stats for non-numeric player id: {}", entry.id());
                }
            }
            rank++;
        }

        log.info("Game results saved: {} players for game {}", result.rankings().size(), gameId);
        return game;
    }

    /**
     * 分页查询游戏列表
     */
    public Map<String, Object> getGameHistory(int page, int size, String mode) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GameEntity> gamePage;

        if (mode != null && !mode.isBlank() && !"all".equals(mode)) {
            gamePage = gameRepository.findByGameMode(mode, pageable);
        } else {
            gamePage = gameRepository.findAll(pageable);
        }

        List<Map<String, Object>> list = gamePage.getContent().stream().map(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("roomId", g.getRoomId());
            m.put("gameMode", g.getGameMode());
            m.put("duration", g.getDuration());
            m.put("playerCount", g.getPlayerCount());
            m.put("status", g.getStatus());
            m.put("startedAt", g.getStartedAt());
            m.put("endedAt", g.getEndedAt());
            m.put("createdAt", g.getCreatedAt());
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", gamePage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", gamePage.getTotalPages());
        return result;
    }

    /**
     * 获取游戏详情（包含玩家成绩）
     */
    public Map<String, Object> getGameDetail(Long gameId) {
        Optional<GameEntity> optGame = gameRepository.findById(gameId);
        if (optGame.isEmpty()) {
            return null;
        }
        GameEntity game = optGame.get();
        List<GamePlayerResult> results = playerResultRepository.findByGameIdOrderByScoreDesc(gameId);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", game.getId());
        detail.put("roomId", game.getRoomId());
        detail.put("gameMode", game.getGameMode());
        detail.put("duration", game.getDuration());
        detail.put("playerCount", game.getPlayerCount());
        detail.put("status", game.getStatus());
        detail.put("startedAt", game.getStartedAt());
        detail.put("endedAt", game.getEndedAt());
        detail.put("createdAt", game.getCreatedAt());

        List<Map<String, Object>> players = results.stream().map(r -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("userId", r.getUserId());
            p.put("score", r.getScore());
            p.put("kills", r.getKills());
            p.put("snakeLength", r.getSnakeLength());
            p.put("survivalTime", r.getSurvivalTime());
            p.put("isAlive", r.isAlive());
            p.put("isBot", r.isBot());
            p.put("rank", r.getRank());
            // 查昵称
            String nickname = null;
            if (!r.isBot()) {
                try {
                    nickname = userRepository.findById(Long.parseLong(r.getUserId()))
                            .map(SysUser::getNickname).orElse(null);
                } catch (NumberFormatException ignored) {}
            }
            p.put("nickname", nickname != null ? nickname : r.getUserId());
            return p;
        }).toList();

        detail.put("players", players);
        return detail;
    }

    /**
     * 获取玩家的历史游戏记录
     */
    public Map<String, Object> getPlayerHistory(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 查询该玩家的所有 GamePlayerResult
        List<Long> gameIds = playerResultRepository.findByUserId(userId).stream()
                .map(GamePlayerResult::getGameId)
                .distinct()
                .toList();

        // 分页取游戏记录
        List<GameEntity> games = gameRepository.findAllById(gameIds).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .skip((long) (page - 1) * size)
                .limit(size)
                .toList();

        int total = gameIds.size();

        List<Map<String, Object>> list = games.stream().map(g -> {
            List<GamePlayerResult> playerResults = playerResultRepository.findByGameIdAndUserId(g.getId(), userId);
            GamePlayerResult myResult = playerResults.isEmpty() ? null : playerResults.get(0);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("roomId", g.getRoomId());
            m.put("gameMode", g.getGameMode());
            m.put("duration", g.getDuration());
            m.put("playerCount", g.getPlayerCount());
            m.put("startedAt", g.getStartedAt());
            m.put("endedAt", g.getEndedAt());
            m.put("createdAt", g.getCreatedAt());
            if (myResult != null) {
                m.put("score", myResult.getScore());
                m.put("kills", myResult.getKills());
                m.put("rank", myResult.getRank());
                m.put("isAlive", myResult.isAlive());
            }
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 获取玩家统计概览
     */
    public Map<String, Object> getPlayerStats(String userId) {
        // 获取该玩家的所有成绩
        List<GamePlayerResult> allResults = playerResultRepository.findByUserId(userId);

        int totalGames = allResults.size();
        int totalScore = allResults.stream().mapToInt(GamePlayerResult::getScore).sum();
        int totalKills = allResults.stream().mapToInt(GamePlayerResult::getKills).sum();
        long wins = allResults.stream().filter(r -> r.getRank() == 1).count();
        int bestScore = allResults.stream().mapToInt(GamePlayerResult::getScore).max().orElse(0);
        double avgScore = totalGames > 0 ? (double) totalScore / totalGames : 0;
        double avgRank = totalGames > 0 ? (double) allResults.stream().mapToInt(GamePlayerResult::getRank).sum() / totalGames : 0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userId", userId);
        stats.put("totalGames", totalGames);
        stats.put("totalScore", totalScore);
        stats.put("totalKills", totalKills);
        stats.put("wins", wins);
        stats.put("bestScore", bestScore);
        stats.put("avgScore", Math.round(avgScore * 10.0) / 10.0);
        stats.put("avgRank", Math.round(avgRank * 10.0) / 10.0);

        // 昵称
        String nickname = null;
        try {
            nickname = userRepository.findById(Long.parseLong(userId))
                    .map(SysUser::getNickname).orElse(null);
        } catch (NumberFormatException ignored) {}
        stats.put("nickname", nickname != null ? nickname : userId);

        return stats;
    }
}
