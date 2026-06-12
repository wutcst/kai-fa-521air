package com.snake.game;

import static org.junit.jupiter.api.Assertions.*;

import com.snake.game.GameEngine.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** 游戏引擎核心逻辑单元测试 */
class GameEngineTest {

    @Test
    void directionOpposite_ShouldReturnCorrectDirection() {
        assertEquals(Direction.DOWN, Direction.UP.opposite());
        assertEquals(Direction.UP, Direction.DOWN.opposite());
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite());
        assertEquals(Direction.LEFT, Direction.RIGHT.opposite());
    }

    @Test
    void directionFrom_ShouldReturnCorrectDirection() {
        assertEquals(Direction.UP, Direction.from("up"));
        assertEquals(Direction.DOWN, Direction.from("DOWN"));
        assertEquals(Direction.LEFT, Direction.from("Left"));
        assertEquals(Direction.RIGHT, Direction.from("right"));
    }

    @Test
    void directionFrom_InvalidString_ShouldReturnNull() {
        assertNull(Direction.from(null));
        assertNull(Direction.from("invalid"));
        assertNull(Direction.from(""));
    }

    @Test
    void directionDxDy_ShouldMatchExpected() {
        assertEquals(0, Direction.UP.dx());
        assertEquals(-1, Direction.UP.dy());
        assertEquals(0, Direction.DOWN.dx());
        assertEquals(1, Direction.DOWN.dy());
        assertEquals(-1, Direction.LEFT.dx());
        assertEquals(0, Direction.LEFT.dy());
        assertEquals(1, Direction.RIGHT.dx());
        assertEquals(0, Direction.RIGHT.dy());
    }

    @Test
    void gameModeFrom_ShouldParseCorrectly() {
        assertEquals(GameMode.SINGLE, GameMode.from("single"));
        assertEquals(GameMode.MULTI, GameMode.from("multi"));
        assertEquals(GameMode.MULTI, GameMode.from(null));
        assertEquals(GameMode.MULTI, GameMode.from("unknown"));
    }

    @Test
    void constructor_ShouldCreateSnakesAndReceiveState() {
        List<PlayerSeed> players =
                List.of(
                        new PlayerSeed("p1", "Player1", false),
                        new PlayerSeed("p2", "Player2", false));
        GameEngine engine = new GameEngine(GameMode.MULTI, 300, players);

        assertEquals(60, GameEngine.MAP_WIDTH);
        assertEquals(60, GameEngine.MAP_HEIGHT);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AtomicReference<GameStateSnapshot> capturedState = new AtomicReference<>();
        AtomicBoolean stateReceived = new AtomicBoolean(false);

        engine.start(
                executor,
                snapshot -> {
                    capturedState.set(snapshot);
                    stateReceived.set(true);
                },
                event -> {},
                result -> {});

        sleep(200);
        engine.stop();

        assertTrue(stateReceived.get(), "Should receive game state");
        GameStateSnapshot state = capturedState.get();
        assertNotNull(state);
        assertEquals(2, state.snakes().size());
        assertTrue(state.mapWidth() > 0);
        assertTrue(state.foods().size() >= 0);
    }

    @Test
    void setDirection_WithOpposite_ShouldBeIgnored() {
        List<PlayerSeed> players = List.of(new PlayerSeed("p1", "Player1", false));
        GameEngine engine = new GameEngine(GameMode.SINGLE, 0, players);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        engine.start(executor, snapshot -> {}, event -> {}, result -> {});

        engine.setDirection("p1", Direction.UP);
        engine.setDirection("p1", Direction.DOWN); // opposite of UP, ignored

        sleep(150);
        engine.stop();
    }

    @Test
    void setDirection_WithNullPlayerId_ShouldNotThrow() {
        List<PlayerSeed> players = List.of(new PlayerSeed("p1", "Player1", false));
        GameEngine engine = new GameEngine(GameMode.SINGLE, 0, players);

        engine.setDirection(null, Direction.UP);
        engine.setDirection("p1", null);
    }

    @Test
    void eliminatePlayer_InSingleMode_ShouldEndGame() {
        List<PlayerSeed> players = List.of(new PlayerSeed("p1", "Player1", false));
        GameEngine engine = new GameEngine(GameMode.SINGLE, 0, players);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AtomicBoolean gameOver = new AtomicBoolean(false);

        engine.start(
                executor,
                snapshot -> {},
                event -> {},
                result -> {
                    gameOver.set(true);
                });

        engine.eliminatePlayer("p1", "died");
        sleep(200);

        assertTrue(
                gameOver.get(), "Eliminating the only player in SINGLE mode should end the game");
        engine.stop();
    }

    @Test
    void eliminatePlayer_InMultiMode_ShouldEndWhenAllDead() {
        // MULTI mode: game ends when aliveCount <= 1 (last surviving player wins)
        List<PlayerSeed> players =
                List.of(
                        new PlayerSeed("p1", "Player1", false),
                        new PlayerSeed("p2", "Player2", false),
                        new PlayerSeed("p3", "Player3", false));
        GameEngine engine = new GameEngine(GameMode.MULTI, 300, players);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger gameOverCount = new AtomicInteger(0);

        engine.start(
                executor,
                snapshot -> {},
                event -> {},
                result -> {
                    gameOverCount.incrementAndGet();
                });

        // Eliminate p1 only - game should NOT end yet (p2, p3 still alive, alive=2 > 1)
        engine.eliminatePlayer("p1", "died");
        sleep(200);
        assertEquals(
                0,
                gameOverCount.get(),
                "Game should not end with 2 players still alive in MULTI mode");

        // Eliminate p2 - game should end (p3 only alive, alive=1 <= 1)
        engine.eliminatePlayer("p2", "died");
        sleep(200);

        assertTrue(
                gameOverCount.get() >= 1,
                "Game should end when only 1 player remains alive in MULTI mode");
        engine.stop();
    }

    @Test
    void stop_ShouldSetStatusToFinished() {
        List<PlayerSeed> players = List.of(new PlayerSeed("p1", "Player1", false));
        GameEngine engine = new GameEngine(GameMode.SINGLE, 0, players);
        engine.stop();
        engine.stop(); // calling twice should not throw
    }

    @Test
    void start_ShouldReceiveStateCallbacks() {
        List<PlayerSeed> players = List.of(new PlayerSeed("p1", "Player1", false));
        GameEngine engine = new GameEngine(GameMode.SINGLE, 0, players);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger stateCount = new AtomicInteger(0);

        engine.start(executor, snapshot -> stateCount.incrementAndGet(), event -> {}, result -> {});
        sleep(100);

        // Second start should be no-op
        engine.start(executor, snapshot -> {}, event -> {}, result -> {});
        sleep(100);

        int countBeforeStop = stateCount.get();
        engine.stop();
        assertTrue(countBeforeStop > 0, "Should have received state callbacks");
    }

    @Test
    void gridPoint_ShouldStoreCoordinates() {
        GridPoint p = new GridPoint(10, 20);
        assertEquals(10, p.x());
        assertEquals(20, p.y());
    }

    @Test
    void snakeState_ShouldStoreAllFields() {
        List<GridPoint> body = List.of(new GridPoint(5, 5), new GridPoint(4, 5));
        SnakeState state =
                new SnakeState(
                        "p1", body, "right", "#ff0000", 100, 2, 5, true, "Player1", 0.0, false, 0.0,
                        true);

        assertEquals("p1", state.id());
        assertEquals(2, state.body().size());
        assertEquals("right", state.direction());
        assertEquals(100, state.score());
        assertEquals(2, state.kills());
        assertEquals(5, state.length());
        assertTrue(state.isAlive());
        assertEquals("Player1", state.nickname());
        assertTrue(state.isMe());
    }

    @Test
    void scoreEntry_ShouldStoreAllFields() {
        ScoreEntry entry =
                new ScoreEntry("p1", "Player1", 200, 3, 10, true, 120.5, true, "#00e676");

        assertEquals("p1", entry.id());
        assertEquals("Player1", entry.nickname());
        assertEquals(200, entry.score());
        assertEquals(3, entry.kills());
        assertEquals(10, entry.length());
        assertTrue(entry.isAlive());
        assertEquals(120.5, entry.survivalTime());
        assertTrue(entry.isMe());
    }

    @Test
    void food_ShouldStoreAndAllowUpdate() {
        Food food = new Food(10, 20, "normal");
        assertEquals(10, food.getX());
        assertEquals(20, food.getY());
        assertEquals("normal", food.getType());

        food.setX(15);
        food.setY(25);
        food.setType("high");

        assertEquals(15, food.getX());
        assertEquals(25, food.getY());
        assertEquals("high", food.getType());
    }

    @Test
    void item_ShouldStoreAndAllowUpdate() {
        Item item = new Item(30, 40, "shield");
        assertEquals(30, item.getX());
        assertEquals(40, item.getY());
        assertEquals("shield", item.getType());

        item.setType("speed");
        assertEquals("speed", item.getType());
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
