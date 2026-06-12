package com.snake.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.snake.entity.GameEntity;
import com.snake.entity.GamePlayerResult;
import com.snake.entity.SysUser;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

/** 排行榜 API 集成测试 */
class RankingIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    void setUpTestData() {
        // 创建测试用户
        SysUser user = new SysUser();
        user.setUsername("rank_user_" + System.currentTimeMillis());
        user.setPassword("$2a$10$encoded");
        user.setNickname("RankUser");
        user.setTotalScore(1000);
        user.setTotalGames(5);
        user.setWins(3);
        user.setLevel(5);
        user.setStatus(1);
        SysUser savedUser = userRepository.save(user);

        // 创建多人游戏
        GameEntity multiGame = new GameEntity();
        multiGame.setRoomId("rank_multi");
        multiGame.setGameMode("multi");
        multiGame.setDuration(300);
        multiGame.setPlayerCount(2);
        multiGame.setStatus("finished");
        multiGame.setStartedAt(LocalDateTime.now().minusHours(1));
        multiGame.setEndedAt(LocalDateTime.now());
        GameEntity savedMulti = gameRepository.save(multiGame);

        GamePlayerResult multiResult = new GamePlayerResult();
        multiResult.setGameId(savedMulti.getId());
        multiResult.setUserId(String.valueOf(savedUser.getId()));
        multiResult.setScore(500);
        multiResult.setKills(3);
        multiResult.setSnakeLength(15);
        multiResult.setSurvivalTime(300.0);
        multiResult.setAlive(true);
        multiResult.setRank(1);
        multiResult.setBot(false);
        playerResultRepository.save(multiResult);

        // 创建单人游戏
        GameEntity singleGame = new GameEntity();
        singleGame.setRoomId("rank_single");
        singleGame.setGameMode("single");
        singleGame.setDuration(120);
        singleGame.setPlayerCount(1);
        singleGame.setStatus("finished");
        singleGame.setStartedAt(LocalDateTime.now().minusHours(2));
        singleGame.setEndedAt(LocalDateTime.now());
        GameEntity savedSingle = gameRepository.save(singleGame);

        GamePlayerResult singleResult = new GamePlayerResult();
        singleResult.setGameId(savedSingle.getId());
        singleResult.setUserId(String.valueOf(savedUser.getId()));
        singleResult.setScore(300);
        singleResult.setKills(1);
        singleResult.setSnakeLength(10);
        singleResult.setSurvivalTime(120.0);
        singleResult.setAlive(true);
        singleResult.setRank(1);
        singleResult.setBot(false);
        playerResultRepository.save(singleResult);
    }

    @Test
    void getOverallRanking_ShouldReturnList() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity(baseUrl() + "/api/ranking", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().get("list"));
    }

    @Test
    void getMultiRanking_ShouldReturnList() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity(baseUrl() + "/api/ranking?mode=multi", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().get("list"));
    }

    @Test
    void getSingleRanking_ShouldReturnList() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity(baseUrl() + "/api/ranking?mode=single", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().get("list"));
    }

    @Test
    void getUserRank_ShouldReturnUserRanking() {
        SysUser anyUser = userRepository.findAll().stream().findFirst().orElse(null);
        if (anyUser == null) {
            fail("No users found - test data not set up");
        }

        ResponseEntity<Map> response =
                restTemplate.getForEntity(
                        baseUrl() + "/api/ranking/user/" + anyUser.getId(), Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().get("overallRank"));
        assertNotNull(response.getBody().get("nickname"));
    }

    @Test
    void getRanking_WithPagination_ShouldRespectPageSize() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity(baseUrl() + "/api/ranking?page=1&size=5", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.get("page"));
        assertEquals(5, body.get("size"));
    }
}
