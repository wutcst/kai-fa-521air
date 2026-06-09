package com.snake.integration;

import com.snake.entity.GameEntity;
import com.snake.entity.GamePlayerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 游戏记录 API 集成测试
 */
class GameIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    void setUpTestData() {
        // 插入一条测试游戏记录
        GameEntity game = new GameEntity();
        game.setRoomId("test_room_1");
        game.setGameMode("single");
        game.setDuration(120);
        game.setPlayerCount(1);
        game.setStatus("finished");
        game.setStartedAt(LocalDateTime.now().minusMinutes(10));
        game.setEndedAt(LocalDateTime.now());
        GameEntity saved = gameRepository.save(game);

        // 插入玩家成绩
        GamePlayerResult result = new GamePlayerResult();
        result.setGameId(saved.getId());
        result.setUserId("999999");
        result.setScore(500);
        result.setKills(3);
        result.setSnakeLength(15);
        result.setSurvivalTime(120.0);
        result.setAlive(true);
        result.setRank(1);
        result.setBot(false);
        playerResultRepository.save(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getGameHistory_ShouldReturnGames() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl() + "/api/games", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Integer) body.get("total") >= 1);
        assertNotNull(body.get("list"));
    }

    @Test
    void getGameHistory_WithModeFilter_ShouldReturnFiltered() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl() + "/api/games?mode=single", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Integer) body.get("total") >= 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getGameDetail_WithExistingGame_ShouldReturnDetail() {
        // 获取第一个游戏 ID
        ResponseEntity<Map> historyResp = restTemplate.getForEntity(
                baseUrl() + "/api/games", Map.class);
        List<Map<String, Object>> list = (List<Map<String, Object>>) historyResp.getBody().get("list");
        assertFalse(list.isEmpty(), "必须有测试游戏数据");
        Long gameId = ((Number) list.get(0).get("id")).longValue();

        // 查询详情
        ResponseEntity<Map> detailResp = restTemplate.getForEntity(
                baseUrl() + "/api/games/" + gameId, Map.class);

        assertEquals(HttpStatus.OK, detailResp.getStatusCode());
        Map<String, Object> detail = detailResp.getBody();
        assertNotNull(detail);
        assertEquals(gameId, ((Number) detail.get("id")).longValue());
        assertNotNull(detail.get("players"));
    }

    @Test
    void getGameDetail_WithNonExistentGame_ShouldReturn404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl() + "/api/games/99999", Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("游戏记录不存在", response.getBody().get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getPlayerStats_ShouldReturnStats() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl() + "/api/games/player/999999/stats", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("999999", body.get("userId"));
        assertTrue((Integer) body.get("totalGames") >= 1);
    }
}
