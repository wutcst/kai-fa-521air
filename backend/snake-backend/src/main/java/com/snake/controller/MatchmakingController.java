package com.snake.controller;

import com.snake.service.MatchmakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 快速匹配 API
 * 提供加入/取消匹配队列、查询匹配状态
 */
@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingController.class);

    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    /**
     * 加入匹配队列
     */
    @PostMapping("/join")
    public ResponseEntity<?> joinQueue(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }

        String userId = String.valueOf(authentication.getPrincipal());
        String nickname = (authentication.getDetails() instanceof String)
                ? (String) authentication.getDetails()
                : userId;

        MatchmakingService.MatchResult result = matchmakingService.joinQueue(userId, nickname);

        if (result.matched()) {
            return ResponseEntity.ok(Map.of(
                    "matched", true,
                    "roomId", result.roomId(),
                    "message", result.message()
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "matched", false,
                    "message", result.message(),
                    "queueSize", matchmakingService.getQueueSize()
            ));
        }
    }

    /**
     * 取消匹配
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelQueue(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }

        String userId = String.valueOf(authentication.getPrincipal());
        matchmakingService.cancelQueue(userId);
        return ResponseEntity.ok(Map.of("message", "已取消匹配"));
    }

    /**
     * 查询匹配状态（轮询用）
     * 返回 matched: true + roomId 表示已匹配成功
     * 返回 matched: false 表示仍在等待
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkStatus(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }

        String userId = String.valueOf(authentication.getPrincipal());
        String roomId = matchmakingService.checkMatchStatus(userId);

        if (roomId != null) {
            return ResponseEntity.ok(Map.of(
                    "matched", true,
                    "roomId", roomId,
                    "message", "匹配成功"
            ));
        }

        boolean inQueue = matchmakingService.isInQueue(userId);
        return ResponseEntity.ok(Map.of(
                "matched", false,
                "inQueue", inQueue,
                "queueSize", matchmakingService.getQueueSize()
        ));
    }
}
