package com.snake.controller;

import com.snake.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 排行榜 API
 * 提供总榜、分模式排行榜、用户排名查询
 */
@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    private static final Logger log = LoggerFactory.getLogger(RankingController.class);

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * 获取排行榜列表
     * @param mode 模式: overall(总榜) / multi(多人) / single(单人)
     * @param page 页码
     * @param size 每页条数
     */
    @GetMapping
    public ResponseEntity<?> getRanking(
            @RequestParam(defaultValue = "overall") String mode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;

        Map<String, Object> result;
        if ("multi".equals(mode) || "single".equals(mode)) {
            result = rankingService.getModeRanking(mode, page, size);
        } else {
            result = rankingService.getOverallRanking(page, size);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前登录用户的排名信息
     */
    @GetMapping("/my-rank")
    public ResponseEntity<?> getMyRank(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }
        String userId = String.valueOf(authentication.getPrincipal());
        return ResponseEntity.ok(rankingService.getMyRank(userId));
    }

    /**
     * 获取指定用户的排名信息
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserRank(@PathVariable String userId) {
        return ResponseEntity.ok(rankingService.getMyRank(userId));
    }
}
