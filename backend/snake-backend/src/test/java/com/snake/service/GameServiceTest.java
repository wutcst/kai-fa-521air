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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 游戏服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerResultRepository playerResultRepository;

    @Mock
    private SysUserRepository userRepository;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, playerResultRepository, userRepository);
    }

    @Test
    void saveGameResult_ShouldSaveGameAndPlayerResults() {
        // Given
        Room room = new Room("room_test_1");
        room.setGameMode("single");
        room.setStartedAt(LocalDateTime.now());

        List<ScoreEntry> rankings = List.of(
            new ScoreEntry("1", "Player1", 500, 3, 15, true, 120.0, false, "#00e676"),
            new ScoreEntry("bot_1", "Bot1", 100, 0, 5, false, 45.0, false, "#448aff")
        );

        GameResult result = new GameResult("game_1", 120, rankings, "single");

        GameEntity savedGame = new GameEntity();
        savedGame.setId(1L);
        savedGame.setRoomId("room_test_1");
        savedGame.setGameMode("single");
        savedGame.setDuration(120);
        savedGame.setPlayerCount(2);
        savedGame.setStatus("finished");
        savedGame.setStartedAt(LocalDateTime.now());
        savedGame.setEndedAt(LocalDateTime.now());

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("player1");
        user.setTotalGames(5);
        user.setTotalScore(1500);
        user.setWins(3);

        when(gameRepository.save(any())).thenReturn(savedGame);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        GameEntity result_game = gameService.saveGameResult(room, result);

        // Then
        assertNotNull(result_game);
        assertEquals(1L, result_game.getId());
        assertEquals("room_test_1", result_game.getRoomId());

        // Verify game saved
        verify(gameRepository, times(1)).save(any(GameEntity.class));

        // Verify player results saved (2 players)
        verify(playerResultRepository, times(2)).save(any(GamePlayerResult.class));

        // Verify user stats updated (1 real user, 1 bot)
        verify(userRepository, times(1)).save(any(SysUser.class));
    }

    @Test
    void getGameHistory_ShouldReturnPaginatedResults() {
        // Given
        GameEntity game = new GameEntity();
        game.setId(1L);
        game.setRoomId("room_1");
        game.setGameMode("single");
        game.setDuration(120);
        game.setPlayerCount(1);
        game.setStatus("finished");

        org.springframework.data.domain.Page<GameEntity> page =
            new org.springframework.data.domain.PageImpl<>(List.of(game));

        when(gameRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        // When
        Map<String, Object> result = gameService.getGameHistory(1, 20, null);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
        assertEquals(1, result.get("page"));
        assertEquals(20, result.get("size"));

        List<?> list = (List<?>) result.get("list");
        assertEquals(1, list.size());

        Map<String, Object> item = (Map<String, Object>) list.get(0);
        assertEquals(1L, item.get("id"));
        assertEquals("room_1", item.get("roomId"));
        assertEquals("single", item.get("gameMode"));
    }

    @Test
    void getGameHistory_WithModeFilter_ShouldCallCorrectRepository() {
        when(gameRepository.findByGameMode(eq("single"), any()))
            .thenReturn(org.springframework.data.domain.Page.empty());

        Map<String, Object> result = gameService.getGameHistory(1, 20, "single");

        assertNotNull(result);
        verify(gameRepository, times(1)).findByGameMode(eq("single"), any());
        verify(gameRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getGameDetail_ShouldReturnGameWithPlayers() {
        // Given
        GameEntity game = new GameEntity();
        game.setId(1L);
        game.setRoomId("room_1");
        game.setGameMode("multi");
        game.setDuration(300);
        game.setPlayerCount(2);
        game.setStatus("finished");

        GamePlayerResult playerResult = new GamePlayerResult();
        playerResult.setId(1L);
        playerResult.setGameId(1L);
        playerResult.setUserId("1");
        playerResult.setScore(500);
        playerResult.setKills(3);
        playerResult.setSnakeLength(15);
        playerResult.setSurvivalTime(120.0);
        playerResult.setAlive(true);
        playerResult.setRank(1);
        playerResult.setBot(false);

        SysUser user = new SysUser();
        user.setId(1L);
        user.setNickname("Player1");

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(playerResultRepository.findByGameIdOrderByScoreDesc(1L)).thenReturn(List.of(playerResult));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        Map<String, Object> detail = gameService.getGameDetail(1L);

        // Then
        assertNotNull(detail);
        assertEquals(1L, detail.get("id"));
        assertEquals("room_1", detail.get("roomId"));

        List<?> players = (List<?>) detail.get("players");
        assertEquals(1, players.size());

        Map<String, Object> player = (Map<String, Object>) players.get(0);
        assertEquals("1", player.get("userId"));
        assertEquals(500, player.get("score"));
        assertEquals("Player1", player.get("nickname"));
    }

    @Test
    void getGameDetail_WithNonExistentGame_ShouldReturnNull() {
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        Map<String, Object> result = gameService.getGameDetail(999L);
        assertNull(result);
    }

    @Test
    void getPlayerStats_ShouldReturnCorrectStats() {
        GamePlayerResult result1 = new GamePlayerResult();
        result1.setUserId("1");
        result1.setScore(500);
        result1.setKills(3);
        result1.setRank(1);

        GamePlayerResult result2 = new GamePlayerResult();
        result2.setUserId("1");
        result2.setScore(200);
        result2.setKills(1);
        result2.setRank(2);

        when(playerResultRepository.findByUserId("1")).thenReturn(List.of(result1, result2));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Map<String, Object> stats = gameService.getPlayerStats("1");

        assertNotNull(stats);
        assertEquals("1", stats.get("userId"));
        assertEquals(2, stats.get("totalGames"));
        assertEquals(700, stats.get("totalScore"));
        assertEquals(4, stats.get("totalKills"));
        assertEquals(1L, stats.get("wins"));  // rank=1 once
        assertEquals(500, stats.get("bestScore"));
    }
}
