package com.snake.service;

import com.snake.entity.GameEntity;
import com.snake.entity.GamePlayerResult;
import com.snake.entity.SysUser;
import com.snake.repository.GamePlayerResultRepository;
import com.snake.repository.GameRepository;
import com.snake.repository.SysUserRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** 排行榜服务 提供总榜、分模式排行榜、当前用户排名查询 */
@Service
public class RankingService {

    private static final Logger log = LoggerFactory.getLogger(RankingService.class);

    private final SysUserRepository userRepository;
    private final GamePlayerResultRepository playerResultRepository;
    private final GameRepository gameRepository;

    public RankingService(
            SysUserRepository userRepository,
            GamePlayerResultRepository playerResultRepository,
            GameRepository gameRepository) {
        this.userRepository = userRepository;
        this.playerResultRepository = playerResultRepository;
        this.gameRepository = gameRepository;
    }

    /**
     * 获取总榜（按累计总分降序）
     *
     * @param page 页码
     * @param size 每页条数
     */
    public Map<String, Object> getOverallRanking(int page, int size) {
        Pageable pageable =
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "totalScore"));
        Page<SysUser> userPage = userRepository.findAllByStatusOrderByTotalScoreDesc(1, pageable);

        List<Map<String, Object>> list =
                userPage.getContent().stream()
                        .map(
                                u -> {
                                    Map<String, Object> m = new LinkedHashMap<>();
                                    m.put("rank", 0); // 后面计算
                                    m.put("userId", String.valueOf(u.getId()));
                                    m.put("nickname", u.getNickname());
                                    m.put("avatar", u.getAvatar());
                                    m.put("totalScore", u.getTotalScore());
                                    m.put("totalGames", u.getTotalGames());
                                    m.put("wins", u.getWins());
                                    m.put("level", u.getLevel());
                                    return m;
                                })
                        .collect(Collectors.toList());

        // 计算实际排名（考虑并列）
        assignRanksByScore(list, (long) (page - 1) * size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", userPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", userPage.getTotalPages());
        return result;
    }

    /**
     * 获取分模式排行榜（按该模式下单局最高分降序） 从 game_player_result 取每个用户的最高单场得分，过滤 bot，按 game_mode 筛选
     *
     * @param mode 游戏模式: multi / single
     * @param page 页码
     * @param size 每页条数
     */
    public Map<String, Object> getModeRanking(String mode, int page, int size) {
        // 1. 查询指定模式下的所有游戏 ID
        List<GameEntity> modeGames = gameRepository.findByGameModeOrderByCreatedAtDesc(mode);
        Set<Long> modeGameIds =
                modeGames.stream().map(GameEntity::getId).collect(Collectors.toSet());

        if (modeGameIds.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("list", Collections.emptyList());
            empty.put("total", 0L);
            empty.put("page", page);
            empty.put("size", size);
            empty.put("totalPages", 0);
            return empty;
        }

        // 2. 查询所有非 bot 的成绩记录
        List<GamePlayerResult> allResults = playerResultRepository.findAll();

        // 3. 按 userId 聚合，仅统计指定模式的游戏，排除 bot
        //    排名依据：单局最高分（bestScore），其余字段为累计汇总
        Map<String, ModeRank> userScores = new HashMap<>();
        for (GamePlayerResult r : allResults) {
            if (r.isBot() || !modeGameIds.contains(r.getGameId())) continue;
            userScores.computeIfAbsent(r.getUserId(), k -> new ModeRank()).add(r);
        }

        // 4. 按单局最高分降序排列
        List<Map<String, Object>> allList =
                userScores.entrySet().stream()
                        .sorted(
                                (a, b) ->
                                        Long.compare(
                                                b.getValue().bestScore, a.getValue().bestScore))
                        .map(
                                e -> {
                                    Map<String, Object> m = new LinkedHashMap<>();
                                    m.put("rank", 0);
                                    m.put("userId", e.getKey());
                                    m.put("totalScore", e.getValue().bestScore); // 排名分数 = 单局最高分
                                    m.put("totalGames", e.getValue().games);
                                    m.put("wins", e.getValue().wins);
                                    m.put("kills", e.getValue().kills);
                                    // 查昵称
                                    String nickname = e.getKey();
                                    try {
                                        nickname =
                                                userRepository
                                                        .findById(Long.parseLong(e.getKey()))
                                                        .map(SysUser::getNickname)
                                                        .orElse(e.getKey());
                                    } catch (NumberFormatException ignored) {
                                    }
                                    m.put("nickname", nickname);
                                    m.put("avatar", "");
                                    m.put("level", 0);
                                    return m;
                                })
                        .collect(Collectors.toList());

        // 5. 分页
        int total = allList.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pageList = allList.subList(fromIndex, toIndex);

        assignRanksByScore(pageList, fromIndex);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", pageList);
        result.put("total", (long) total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        return result;
    }

    /**
     * 获取当前用户的排名信息
     *
     * @param userId 用户ID（String 形式）
     */
    public Map<String, Object> getMyRank(String userId) {
        Map<String, Object> myRank = new LinkedHashMap<>();
        myRank.put("userId", userId);

        // 查用户信息
        SysUser user = null;
        try {
            user = userRepository.findById(Long.parseLong(userId)).orElse(null);
        } catch (NumberFormatException ignored) {
        }

        if (user != null) {
            myRank.put("nickname", user.getNickname());
            myRank.put("totalScore", user.getTotalScore());
            myRank.put("totalGames", user.getTotalGames());
            myRank.put("wins", user.getWins());
            myRank.put("level", user.getLevel());
            myRank.put("avatar", user.getAvatar());

            // 总榜排名：计算有多少人总分比当前用户高
            long overallRank =
                    userRepository.countByTotalScoreGreaterThan(user.getTotalScore()) + 1;
            myRank.put("overallRank", overallRank);

            // 胜场排名
            long winRank = userRepository.countByWinsGreaterThan(user.getWins()) + 1;
            myRank.put("winRank", winRank);
        } else {
            myRank.put("nickname", userId);
            myRank.put("totalScore", 0);
            myRank.put("totalGames", 0);
            myRank.put("wins", 0);
            myRank.put("level", 1);
            myRank.put("avatar", "");
            myRank.put("overallRank", 0);
            myRank.put("winRank", 0);
        }

        // 多人模式排名
        long multiRank = computeModeRank(userId, "multi");
        myRank.put("multiRank", multiRank);

        // 单人模式排名
        long singleRank = computeModeRank(userId, "single");
        myRank.put("singleRank", singleRank);

        return myRank;
    }

    // ---- 内部辅助 ----

    /** 给列表中的每一项计算排名序号 */
    private void assignRanksByScore(List<Map<String, Object>> list, long offset) {
        long prevScore = Long.MIN_VALUE;
        long rank = offset;
        long sameCount = 0;
        for (Map<String, Object> item : list) {
            long score = ((Number) item.getOrDefault("totalScore", 0)).longValue();
            if (score != prevScore) {
                rank = rank + sameCount + 1;
                sameCount = 0;
                prevScore = score;
            } else {
                sameCount++;
            }
            item.put("rank", rank);
        }
    }

    /** 计算用户在指定模式下的排名（按单局最高分） */
    private long computeModeRank(String userId, String mode) {
        List<GameEntity> modeGames = gameRepository.findByGameModeOrderByCreatedAtDesc(mode);
        Set<Long> modeGameIds =
                modeGames.stream().map(GameEntity::getId).collect(Collectors.toSet());

        if (modeGameIds.isEmpty()) return 0;

        List<GamePlayerResult> allResults = playerResultRepository.findAll();

        // 取每个非 bot 用户的单局最高分
        Map<String, Long> userBestScores = new HashMap<>();
        for (GamePlayerResult r : allResults) {
            if (r.isBot() || !modeGameIds.contains(r.getGameId())) continue;
            userBestScores.merge(r.getUserId(), (long) r.getScore(), Math::max);
        }

        long myBestScore = userBestScores.getOrDefault(userId, 0L);
        if (myBestScore == 0) return 0;

        // 计算有多少人单局最高分比我高
        long rank = userBestScores.values().stream().filter(s -> s > myBestScore).count() + 1;
        return rank;
    }

    /** 内部类：模式排名聚合（排名依据单局最高分，其余字段累计） */
    private static class ModeRank {
        long bestScore = 0; // 单局最高分（排名依据）
        int games = 0;
        int wins = 0;
        int kills = 0;

        void add(GamePlayerResult r) {
            if (r.getScore() > bestScore) {
                bestScore = r.getScore();
            }
            games++;
            kills += r.getKills();
            if (r.getRank() == 1) wins++;
        }
    }
}
