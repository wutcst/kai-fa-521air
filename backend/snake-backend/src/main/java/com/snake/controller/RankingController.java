package com.snake.controller;

import com.snake.service.RankingService;
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

/** 排行榜 API 提供总榜、分模式排行榜、用户排名查询 */
@RestController
@RequestMapping("/api/ranking")
@Tag(name = "03-排行榜", description = "总榜、分模式排行榜、用户排名查询")
public class RankingController {

    private static final Logger log = LoggerFactory.getLogger(RankingController.class);

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    @Operation(
            summary = "获取排行榜列表",
            description = "分页查询排行榜，可选模式：overall(总榜) / multi(多人) / single(单人)")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "成功返回排行榜",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"list\":[],\"total\":0,\"page\":1,\"size\":20,\"totalPages\":0}")))
    })
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

    @GetMapping("/my-rank")
    @Operation(summary = "获取当前用户的排名", description = "需要 JWT token，返回当前登录用户的排名信息（总榜排名、各类统计等）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功返回排名信息"),
        @ApiResponse(
                responseCode = "401",
                description = "未登录",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"未登录\"}")))
    })
    public ResponseEntity<?> getMyRank(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }
        String userId = String.valueOf(authentication.getPrincipal());
        return ResponseEntity.ok(rankingService.getMyRank(userId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取指定用户的排名", description = "按用户ID查询该用户的排名信息")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "成功返回排名信息")})
    public ResponseEntity<?> getUserRank(@PathVariable String userId) {
        return ResponseEntity.ok(rankingService.getMyRank(userId));
    }
}
