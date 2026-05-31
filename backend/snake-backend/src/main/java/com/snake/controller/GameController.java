package com.snake.controller;

import com.snake.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 游戏状态存储与查询 API
 * 提供游戏历史、详情、玩家统计等接口
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * 获取游戏历史列表
     */
    @GetMapping
    public ResponseEntity<?> getGameHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String mode) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        return ResponseEntity.ok(gameService.getGameHistory(page, size, mode));
    }

    /**
     * 获取游戏详情（含玩家成绩）
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<?> getGameDetail(@PathVariable Long gameId) {
        Map<String, Object> detail = gameService.getGameDetail(gameId);
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "游戏记录不存在"));
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * 获取当前登录用户的游戏历史
     */
    @GetMapping("/my-history")
    public ResponseEntity<?> getMyHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }
        String userId = String.valueOf(authentication.getPrincipal());
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        return ResponseEntity.ok(gameService.getPlayerHistory(userId, page, size));
    }

    /**
     * 获取当前登录用户的统计概览
     */
    @GetMapping("/my-stats")
    public ResponseEntity<?> getMyStats(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }
        String userId = String.valueOf(authentication.getPrincipal());
        return ResponseEntity.ok(gameService.getPlayerStats(userId));
    }

    /**
     * 获取指定玩家的游戏历史
     */
    @GetMapping("/player/{userId}/history")
    public ResponseEntity<?> getPlayerHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        return ResponseEntity.ok(gameService.getPlayerHistory(userId, page, size));
    }

    /**
     * 获取指定玩家的统计概览
     */
    @GetMapping("/player/{userId}/stats")
    public ResponseEntity<?> getPlayerStats(@PathVariable String userId) {
        return ResponseEntity.ok(gameService.getPlayerStats(userId));
    }
}
