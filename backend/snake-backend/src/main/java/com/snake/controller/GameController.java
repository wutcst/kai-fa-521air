package com.snake.controller;

import com.snake.service.GameService;
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

/** 游戏状态存储与查询 API 提供游戏历史、详情、玩家统计等接口 */
@RestController
@RequestMapping("/api/games")
@Tag(name = "02-游戏记录", description = "游戏历史查询、详情查看、玩家统计")
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    @Operation(summary = "获取游戏历史列表", description = "分页查询游戏记录，可选按游戏模式筛选")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "成功返回游戏历史",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"list\":[],\"total\":0,\"page\":1,\"size\":20,\"totalPages\":0}")))
    })
    public ResponseEntity<?> getGameHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String mode) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        return ResponseEntity.ok(gameService.getGameHistory(page, size, mode));
    }

    @GetMapping("/{gameId}")
    @Operation(summary = "获取游戏详情", description = "按游戏ID查询单局游戏详情，包含所有玩家的成绩")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功返回游戏详情"),
        @ApiResponse(
                responseCode = "404",
                description = "游戏记录不存在",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"游戏记录不存在\"}")))
    })
    public ResponseEntity<?> getGameDetail(@PathVariable Long gameId) {
        Map<String, Object> detail = gameService.getGameDetail(gameId);
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "游戏记录不存在"));
        }
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/my-history")
    @Operation(summary = "获取当前用户的游戏历史", description = "需要 JWT token，返回当前用户的游戏历史记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功返回游戏历史"),
        @ApiResponse(
                responseCode = "401",
                description = "未登录",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"未登录\"}")))
    })
    public ResponseEntity<?> getMyHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }
        String userId = String.valueOf(authentication.getPrincipal());
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        return ResponseEntity.ok(gameService.getPlayerHistory(userId, page, size));
    }

    @GetMapping("/my-stats")
    @Operation(summary = "获取当前用户的统计概览", description = "需要 JWT token，返回当前用户的游戏统计信息（总局数、胜场、总得分等）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功返回统计概览"),
        @ApiResponse(
                responseCode = "401",
                description = "未登录",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"未登录\"}")))
    })
    public ResponseEntity<?> getMyStats(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }
        String userId = String.valueOf(authentication.getPrincipal());
        return ResponseEntity.ok(gameService.getPlayerStats(userId));
    }

    @GetMapping("/player/{userId}/history")
    @Operation(summary = "获取指定玩家的游戏历史", description = "按用户ID查询该玩家的游戏历史记录，分页返回")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "成功返回游戏历史",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"list\":[],\"total\":0,\"page\":1,\"size\":20,\"totalPages\":0}")))
    })
    public ResponseEntity<?> getPlayerHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        return ResponseEntity.ok(gameService.getPlayerHistory(userId, page, size));
    }

    @GetMapping("/player/{userId}/stats")
    @Operation(summary = "获取指定玩家的统计概览", description = "按用户ID查询该玩家的游戏统计信息")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "成功返回统计概览")})
    public ResponseEntity<?> getPlayerStats(@PathVariable String userId) {
        return ResponseEntity.ok(gameService.getPlayerStats(userId));
    }
}
