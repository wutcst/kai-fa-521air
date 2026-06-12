package com.snake.controller;

import com.snake.service.MatchmakingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 快速匹配 API 提供加入/取消匹配队列、查询匹配状态 */
@RestController
@RequestMapping("/api/matchmaking")
@Tag(name = "05-匹配管理", description = "快速匹配队列管理：加入/取消匹配、查询匹配状态")
public class MatchmakingController {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingController.class);

    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/join")
    @Operation(summary = "加入匹配队列", description = "需要 JWT token，将当前用户加入快速匹配队列，匹配成功时返回 roomId")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "成功加入队列（matched=false 表示等待中，matched=true 表示已匹配到房间）",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"matched\":false,\"message\":\"已加入匹配队列\",\"queueSize\":3}"))),
        @ApiResponse(
                responseCode = "401",
                description = "未登录",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"未登录\"}")))
    })
    public ResponseEntity<?> joinQueue(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }

        String userId = String.valueOf(authentication.getPrincipal());
        String nickname =
                (authentication.getDetails() instanceof String)
                        ? (String) authentication.getDetails()
                        : userId;

        MatchmakingService.MatchResult result = matchmakingService.joinQueue(userId, nickname);

        if (result.matched()) {
            return ResponseEntity.ok(
                    Map.of(
                            "matched", true,
                            "roomId", result.roomId(),
                            "message", result.message()));
        } else {
            return ResponseEntity.ok(
                    Map.of(
                            "matched", false,
                            "message", result.message(),
                            "queueSize", matchmakingService.getQueueSize()));
        }
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消匹配", description = "需要 JWT token，将当前用户从匹配队列中移除")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "已取消匹配",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"已取消匹配\"}"))),
        @ApiResponse(
                responseCode = "401",
                description = "未登录",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"未登录\"}")))
    })
    public ResponseEntity<?> cancelQueue(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }

        String userId = String.valueOf(authentication.getPrincipal());
        matchmakingService.cancelQueue(userId);
        return ResponseEntity.ok(Map.of("message", "已取消匹配"));
    }

    @GetMapping("/status")
    @Operation(
            summary = "查询匹配状态",
            description = "轮询用。返回 matched: true + roomId 表示已匹配成功；matched: false 表示仍在等待队列中")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "成功返回匹配状态",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"matched\":false,\"inQueue\":true,\"queueSize\":3}"))),
        @ApiResponse(
                responseCode = "401",
                description = "未登录",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"未登录\"}")))
    })
    public ResponseEntity<?> checkStatus(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }

        String userId = String.valueOf(authentication.getPrincipal());
        String roomId = matchmakingService.checkMatchStatus(userId);

        if (roomId != null) {
            return ResponseEntity.ok(Map.of("matched", true, "roomId", roomId, "message", "匹配成功"));
        }

        boolean inQueue = matchmakingService.isInQueue(userId);
        return ResponseEntity.ok(
                Map.of(
                        "matched",
                        false,
                        "inQueue",
                        inQueue,
                        "queueSize",
                        matchmakingService.getQueueSize()));
    }
}
