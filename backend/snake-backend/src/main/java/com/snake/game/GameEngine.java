package com.snake.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GameEngine {
    public static final int MAP_WIDTH = 60;
    public static final int MAP_HEIGHT = 60;
    public static final int GRID_SIZE = 20;
    public static final int TICK_RATE = 10;
    private static final int TICK_INTERVAL_MS = 1000 / TICK_RATE;

    private static final String[] SNAKE_COLORS = {
        "#00e676", "#448aff", "#ff6e40", "#e040fb",
        "#ffd740", "#69f0ae", "#ff5252", "#40c4ff"
    };

    private static final org.slf4j.Logger engineLog = org.slf4j.LoggerFactory.getLogger(GameEngine.class);

    public enum GameMode {
        SINGLE,
        MULTI;

        public static GameMode from(String value) {
            if (value == null) {
                return MULTI;
            }
            return "single".equalsIgnoreCase(value) ? SINGLE : MULTI;
        }
    }

    public enum Direction {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public int dx() {
            return dx;
        }

        public int dy() {
            return dy;
        }

        public Direction opposite() {
            return switch (this) {
                case UP -> DOWN;
                case DOWN -> UP;
                case LEFT -> RIGHT;
                case RIGHT -> LEFT;
            };
        }

        public static Direction from(String value) {
            if (value == null) {
                return null;
            }
            return switch (value.toLowerCase()) {
                case "up" -> UP;
                case "down" -> DOWN;
                case "left" -> LEFT;
                case "right" -> RIGHT;
                default -> null;
            };
        }
    }

    public record PlayerSeed(String id, String nickname, boolean bot) {
    }

    public record GridPoint(int x, int y) {
    }

    public static final class Food {
        private int x;
        private int y;
        private String type;

        public Food(int x, int y, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static final class Item {
        private int x;
        private int y;
        private String type;

        public Item(int x, int y, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static final class Obstacle {
        private final int x;
        private final int y;

        public Obstacle(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    public record SnakeState(
        String id,
        List<GridPoint> body,
        String direction,
        String color,
        int score,
        int kills,
        int length,
        boolean isAlive,
        String nickname,
        double speedBoost,
        boolean shield,
        double magnet,
        boolean isMe
    ) {
    }

    public record ScoreEntry(
        String id,
        String nickname,
        int score,
        int kills,
        int length,
        boolean isAlive,
        double survivalTime,
        boolean isMe,
        String color
    ) {
    }

    public record GameStateSnapshot(
        Map<String, SnakeState> snakes,
        List<Food> foods,
        List<Item> items,
        List<Obstacle> obstacles,
        List<ScoreEntry> scoreBoard,
        double gameTime,
        double totalTime,
        String gameStatus,
        int mapWidth,
        int mapHeight,
        int gridSize
    ) {
    }

    public record GameResult(String gameId, int duration, List<ScoreEntry> rankings, String gameMode) {
    }

    public record GameEvent(String type, Map<String, Object> data) {
    }

    private static final class Snake {
        private final String id;
        private final boolean bot;
        private final String nickname;
        private final String color;
        private final List<GridPoint> body;
        private Direction direction;
        private Direction nextDirection;
        private int score;
        private int kills;
        private boolean isAlive;
        private double deathTime;
        private double speedBoost;
        private boolean shield;
        private double magnet;
        private double speed;
        private double moveAccum;

        private Snake(String id, String nickname, String color, boolean bot, List<GridPoint> body, Direction direction) {
            this.id = id;
            this.nickname = nickname;
            this.color = color;
            this.bot = bot;
            this.body = body;
            this.direction = direction;
            this.nextDirection = direction;
            this.score = 0;
            this.kills = 0;
            this.isAlive = true;
            this.deathTime = 0;
            this.speedBoost = 0;
            this.shield = false;
            this.magnet = 0;
            this.speed = 0.5;
            this.moveAccum = 0;
        }
    }

    private final Object lock = new Object();
    private final GameMode mode;
    private final int gameDurationSeconds;
    private final Map<String, Snake> snakes = new LinkedHashMap<>();
    private final List<Food> foods = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private double gameTime;
    private String status = "idle";
    private long lastTickNanos;
    private double survivalTimer;
    private ScheduledFuture<?> tickTask;
    private ScheduledFuture<?> foodTask;
    private ScheduledFuture<?> itemTask;
    private Consumer<GameStateSnapshot> stateCallback;
    private Consumer<GameEvent> eventCallback;
    private Consumer<GameResult> gameOverCallback;

    public GameEngine(GameMode mode, int gameDurationSeconds, List<PlayerSeed> players) {
        this.mode = mode == null ? GameMode.MULTI : mode;
        this.gameDurationSeconds = gameDurationSeconds;
        initGame(players);
    }

    public void start(
        ScheduledExecutorService scheduler,
        Consumer<GameStateSnapshot> stateCallback,
        Consumer<GameEvent> eventCallback,
        Consumer<GameResult> gameOverCallback
    ) {
        this.stateCallback = stateCallback;
        this.eventCallback = eventCallback;
        this.gameOverCallback = gameOverCallback;

        synchronized (lock) {
            if ("playing".equals(status)) {
                return;
            }
            status = "playing";
            lastTickNanos = System.nanoTime();
        }

        tickTask = scheduler.scheduleAtFixedRate(this::tick, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        itemTask = scheduler.scheduleAtFixedRate(this::spawnItems, 5, 5, TimeUnit.SECONDS);
        foodTask = scheduler.scheduleAtFixedRate(this::spawnFoods, 2, 2, TimeUnit.SECONDS);
        broadcastState();
    }

    public void stop() {
        synchronized (lock) {
            status = "finished";
        }
        stopTimers();
    }

    public void setDirection(String playerId, Direction direction) {
        if (direction == null || playerId == null) {
            return;
        }
        synchronized (lock) {
            Snake snake = snakes.get(playerId);
            if (snake == null || !snake.isAlive) {
                return;
            }
            if (direction != snake.direction.opposite()) {
                snake.nextDirection = direction;
            }
        }
    }

    public void useItem(String playerId, String itemType) {
        if (playerId == null || itemType == null) {
            return;
        }
        synchronized (lock) {
            Snake snake = snakes.get(playerId);
            if (snake == null || !snake.isAlive) {
                return;
            }
            applyItem(snake, itemType);
        }
    }

    public void eliminatePlayer(String playerId, String reason) {
        if (playerId == null) {
            return;
        }
        synchronized (lock) {
            Snake snake = snakes.get(playerId);
            if (snake == null || !snake.isAlive) {
                return;
            }
            eliminateSnake(snake, reason == null ? "disconnected" : reason);
        }
    }

    private void initGame(List<PlayerSeed> players) {
        obstacles.addAll(generateObstacles(5));
        List<GridPoint> positions = getSpawnPositions(players.size());
        for (int i = 0; i < players.size(); i++) {
            PlayerSeed seed = players.get(i);
            GridPoint pos = positions.get(i);
            Direction initial = getInitialDirection(pos);
            List<GridPoint> body = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                body.add(new GridPoint(pos.x() - initial.dx() * j, pos.y() - initial.dy() * j));
            }
            String color = SNAKE_COLORS[i % SNAKE_COLORS.length];
            Snake snake = new Snake(seed.id(), seed.nickname(), color, seed.bot(), body, initial);
            snakes.put(seed.id(), snake);
        }
        foods.addAll(generateFoods(20));
        items.addAll(generateItems(3));
        gameTime = 0;
        status = "idle";
        lastTickNanos = System.nanoTime();
    }

    private void tick() {
        synchronized (lock) {
            if (!"playing".equals(status)) {
                return;
            }

            long now = System.nanoTime();
            double deltaTime = (now - lastTickNanos) / 1_000_000_000.0;
            lastTickNanos = now;
            gameTime += deltaTime;

            if (mode == GameMode.MULTI && gameDurationSeconds > 0 && gameTime >= gameDurationSeconds) {
                endGame();
                return;
            }

            if (mode == GameMode.MULTI) {
                long aliveCount = snakes.values().stream().filter(s -> s.isAlive).count();
                if (aliveCount <= 1) {
                    if (aliveCount == 1) {
                        snakes.values().stream().filter(s -> s.isAlive).findFirst().ifPresent(s -> s.score += 200);
                    }
                    endGame();
                    return;
                }
            }

            for (Snake snake : snakes.values()) {
                if (!snake.isAlive) {
                    continue;
                }
                if (snake.bot) {
                    updateAIDirection(snake);
                }
                snake.direction = snake.nextDirection;

                double baseSpeed = snake.speed * (snake.speedBoost > 0 ? 1.5 : 1.0);
                snake.moveAccum += baseSpeed;
                int steps = (int) Math.floor(snake.moveAccum);
                snake.moveAccum -= steps;

                for (int s = 0; s < steps; s++) {
                    if (!snake.isAlive) {
                        break;
                    }
                    moveSnake(snake);
                }

                if (snake.speedBoost > 0) {
                    snake.speedBoost = Math.max(0, snake.speedBoost - deltaTime);
                }
                if (snake.magnet > 0) {
                    snake.magnet = Math.max(0, snake.magnet - deltaTime);
                }
                if (snake.magnet > 0) {
                    applyMagnetEffect(snake);
                }
            }

            if (foods.size() < 20) {
                foods.addAll(generateFoods(3));
            }

            survivalTimer += deltaTime;
            if (survivalTimer >= 30) {
                survivalTimer -= 30;
                for (Snake snake : snakes.values()) {
                    if (snake.isAlive) {
                        snake.score += 20;
                    }
                }
            }

            broadcastState();
        }
    }

    private void moveSnake(Snake snake) {
        GridPoint head = snake.body.get(0);
        int newX = head.x() + snake.direction.dx();
        int newY = head.y() + snake.direction.dy();

        if (newX < 0 || newX >= MAP_WIDTH || newY < 0 || newY >= MAP_HEIGHT) {
            newX = ((newX % MAP_WIDTH) + MAP_WIDTH) % MAP_WIDTH;
            newY = ((newY % MAP_HEIGHT) + MAP_HEIGHT) % MAP_HEIGHT;
        }

        GridPoint newHead = new GridPoint(newX, newY);

        for (Obstacle obstacle : obstacles) {
            if (obstacle.getX() == newX && obstacle.getY() == newY) {
                eliminateSnake(snake, "hit obstacle");
                return;
            }
        }

        for (Snake other : snakes.values()) {
            if (other == snake || !other.isAlive) {
                continue;
            }

            GridPoint otherHead = other.body.get(0);
            if (samePos(newHead, otherHead)) {
                if (snake.body.size() >= other.body.size()) {
                    eliminateSnake(other, "head to head");
                    snake.score += 100;
                    snake.kills++;
                    emitEvent("player_kill", Map.of("killerId", snake.id, "victimId", other.id, "method", "head"));
                    spawnDeathFoods(other);
                } else {
                    eliminateSnake(snake, "head to head");
                    other.score += 100;
                    other.kills++;
                    emitEvent("player_kill", Map.of("killerId", other.id, "victimId", snake.id, "method", "head"));
                    spawnDeathFoods(snake);
                    return;
                }
                continue;
            }

            for (GridPoint segment : other.body) {
                if (samePos(newHead, segment)) {
                    if (snake.shield) {
                        snake.shield = false;
                        emitEvent("shield_used", Map.of("snakeId", snake.id));
                    } else {
                        eliminateSnake(snake, "hit body");
                        other.score += 100;
                        other.kills++;
                        emitEvent("player_kill", Map.of("killerId", other.id, "victimId", snake.id, "method", "body"));
                        spawnDeathFoods(snake);
                        return;
                    }
                    break;
                }
            }
        }

        for (int i = foods.size() - 1; i >= 0; i--) {
            Food food = foods.get(i);
            if (food.getX() == newX && food.getY() == newY) {
                snake.body.add(0, newHead);
                snake.body.add(0, new GridPoint(newX, newY));
                snake.score += "high".equals(food.getType()) ? 50 : 10;
                foods.remove(i);
                snake.speed = Math.min(2.0, snake.speed + 0.02);
                break;
            }
        }

        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            if (item.getX() == newX && item.getY() == newY) {
                applyItem(snake, item.getType());
                items.remove(i);
                emitEvent("item_picked", Map.of("snakeId", snake.id, "itemType", item.getType()));
                break;
            }
        }

        snake.body.add(0, newHead);
        snake.body.remove(snake.body.size() - 1);
    }

    // ================================================================
    //  AI 机器人 —— 增强版智能决策
    // ================================================================

    /** 每 N 个 tick 重新评估一次方向（tick 约 100ms，即约 0.2s 决策一次） */
    private static final int AI_DECISION_INTERVAL = 2;
    /** 安全前瞻步数 */
    private static final int AI_LOOKAHEAD = 8;
    /** BFS 搜索深度上限 */
    private static final int AI_BFS_MAX_DEPTH = 30;

    private void updateAIDirection(Snake snake) {
        // 降频决策：不需要每帧都算
        if (tickCounter() % AI_DECISION_INTERVAL != 0) {
            return;
        }

        GridPoint head = snake.body.get(0);

        // 1. 收集所有候选方向（排除反向）
        List<Direction> candidates = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (dir != snake.direction.opposite()) {
                candidates.add(dir);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        // 2. 对每个方向做安全评估 + 可达格数（flood fill）
        int[] scores = new int[candidates.size()];
        int maxScore = Integer.MIN_VALUE;
        int bestIdx = 0;

        for (int i = 0; i < candidates.size(); i++) {
            Direction dir = candidates.get(i);
            GridPoint next = wrap(head.x() + dir.dx(), head.y() + dir.dy());

            // 2a. 立即危险检测（撞障碍物 / 撞蛇身）
            if (isCellDeadly(next, snake)) {
                scores[i] = -10000;
                continue;
            }

            // 2b. 前瞻检测：模拟向前走几步看是否安全
            int lookaheadSafety = evaluateLookahead(next, dir, snake);
            if (lookaheadSafety < 0) {
                scores[i] = -5000 + lookaheadSafety; // 不安全但未立即死亡
                continue;
            }

            // 2c. BFS 计算该方向可达区域大小
            int reachable = floodFillCount(next, snake);
            scores[i] = reachable;

            // 2d. 食物吸引加分
            scores[i] += evaluateFoodAttraction(next, snake);

            // 2e. 威胁躲避：远离比自己长的蛇头
            scores[i] += evaluateThreatAvoidance(next, snake);

            // 2f. 道具吸引
            scores[i] += evaluateItemAttraction(next, snake);

            if (scores[i] > maxScore) {
                maxScore = scores[i];
                bestIdx = i;
            }
        }

        // 3. 如果所有方向都不安全，选相对最好的
        if (maxScore < -5000) {
            // 所有方向都危险，选可达格子最多的
            int bestReachable = -1;
            for (int i = 0; i < candidates.size(); i++) {
                Direction dir = candidates.get(i);
                GridPoint next = wrap(head.x() + dir.dx(), head.y() + dir.dy());
                if (!isCellDeadly(next, snake)) {
                    int r = floodFillCount(next, snake);
                    if (r > bestReachable) {
                        bestReachable = r;
                        bestIdx = i;
                    }
                }
            }
        }

        snake.nextDirection = candidates.get(bestIdx);
    }

    /** 简易 tick 计数器（基于 gameTime） */
    private long tickCounter() {
        return Math.round(gameTime * TICK_RATE);
    }

    /** 坐标环绕 */
    private GridPoint wrap(int x, int y) {
        int wx = ((x % MAP_WIDTH) + MAP_WIDTH) % MAP_WIDTH;
        int wy = ((y % MAP_HEIGHT) + MAP_HEIGHT) % MAP_HEIGHT;
        return new GridPoint(wx, wy);
    }

    /** 判断某个格子是否致命（障碍物 / 任何蛇身） */
    private boolean isCellDeadly(GridPoint cell, Snake self) {
        // 障碍物
        for (Obstacle ob : obstacles) {
            if (ob.getX() == cell.x() && ob.getY() == cell.y()) {
                return true;
            }
        }
        // 所有蛇身（包括自己和其他蛇）
        for (Snake s : snakes.values()) {
            if (!s.isAlive) continue;
            int start = (s == self) ? 1 : 0; // 自己的尾巴尖不算（下一步会移走）
            for (int i = start; i < s.body.size(); i++) {
                GridPoint seg = s.body.get(i);
                if (seg.x() == cell.x() && seg.y() == cell.y()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 判断某个格子是否是蛇身（用于 BFS 阻挡判断，包含自己整条蛇） */
    private boolean isOccupied(GridPoint cell, Snake self, boolean includeSelf) {
        for (Obstacle ob : obstacles) {
            if (ob.getX() == cell.x() && ob.getY() == cell.y()) return true;
        }
        for (Snake s : snakes.values()) {
            if (!s.isAlive) continue;
            if (!includeSelf && s == self) continue;
            for (GridPoint seg : s.body) {
                if (seg.x() == cell.x() && seg.y() == cell.y()) return true;
            }
        }
        return false;
    }

    /** 前瞻评估：从 next 位置开始，沿 dir 方向模拟 lookahead 步 */
    private int evaluateLookahead(GridPoint start, Direction dir, Snake self) {
        int x = start.x();
        int y = start.y();
        int safety = AI_LOOKAHEAD;

        // 模拟蛇向前移动：头部前进，尾部缩短
        List<GridPoint> virtualBody = new ArrayList<>(self.body);
        for (int step = 0; step < AI_LOOKAHEAD; step++) {
            x = ((x + dir.dx()) % MAP_WIDTH + MAP_WIDTH) % MAP_WIDTH;
            y = ((y + dir.dy()) % MAP_HEIGHT + MAP_HEIGHT) % MAP_HEIGHT;
            GridPoint nextHead = new GridPoint(x, y);

            // 检查障碍物
            for (Obstacle ob : obstacles) {
                if (ob.getX() == x && ob.getY() == y) {
                    return -AI_LOOKAHEAD + step;
                }
            }
            // 检查蛇身（不计自己尾部缩短的部分）
            virtualBody.add(0, nextHead);
            if (virtualBody.size() > self.body.size()) {
                virtualBody.remove(virtualBody.size() - 1);
            }
            // 检查自碰（跳过头部自己）
            for (int i = 1; i < virtualBody.size(); i++) {
                if (virtualBody.get(i).x() == x && virtualBody.get(i).y() == y) {
                    return -AI_LOOKAHEAD + step;
                }
            }
            // 检查撞其他蛇
            for (Snake other : snakes.values()) {
                if (other == self || !other.isAlive) continue;
                for (GridPoint seg : other.body) {
                    if (seg.x() == x && seg.y() == y) {
                        return -AI_LOOKAHEAD + step;
                    }
                }
            }
        }
        return safety;
    }

    /** BFS 计算从起点出发可达的空格数量 */
    private int floodFillCount(GridPoint start, Snake self) {
        boolean[][] visited = new boolean[MAP_WIDTH][MAP_HEIGHT];
        java.util.ArrayDeque<GridPoint> queue = new java.util.ArrayDeque<>();
        queue.add(start);
        visited[start.x()][start.y()] = true;
        int count = 0;

        while (!queue.isEmpty() && count < AI_BFS_MAX_DEPTH * 4) {
            GridPoint cur = queue.poll();
            count++;

            for (Direction dir : Direction.values()) {
                int nx = ((cur.x() + dir.dx()) % MAP_WIDTH + MAP_WIDTH) % MAP_WIDTH;
                int ny = ((cur.y() + dir.dy()) % MAP_HEIGHT + MAP_HEIGHT) % MAP_HEIGHT;
                if (!visited[nx][ny]) {
                    GridPoint np = new GridPoint(nx, ny);
                    if (!isOccupied(np, self, true)) {
                        visited[nx][ny] = true;
                        queue.add(np);
                    }
                }
            }
        }
        return count;
    }

    /** 食物吸引力评分 */
    private int evaluateFoodAttraction(GridPoint pos, Snake self) {
        int score = 0;

        for (Food food : foods) {
            int dist = manhattan(pos.x(), pos.y(), food.getX(), food.getY());
            if (dist < 15) {
                int foodScore = "high".equals(food.getType()) ? 80 : 30;
                // 距离越近分越高
                score += Math.max(0, foodScore - dist * 3);
            }
        }

        // 如果蛇很长，降低食物优先级（更注重生存）
        if (self.body.size() > 15) {
            score = score / 2;
        }

        return score;
    }

    /** 道具吸引力评分 */
    private int evaluateItemAttraction(GridPoint pos, Snake self) {
        int score = 0;
        for (Item item : items) {
            int dist = manhattan(pos.x(), pos.y(), item.getX(), item.getY());
            if (dist < 12) {
                score += Math.max(0, 50 - dist * 4);
            }
        }
        return score;
    }

    /** 威胁躲避：远离比自己头更大的蛇 */
    private int evaluateThreatAvoidance(GridPoint pos, Snake self) {
        int score = 0;
        for (Snake other : snakes.values()) {
            if (other == self || !other.isAlive) continue;
            GridPoint otherHead = other.body.get(0);
            int dist = manhattan(pos.x(), pos.y(), otherHead.x(), otherHead.y());

            if (dist < 5) {
                if (other.body.size() >= self.body.size()) {
                    // 对方不比自己小 → 很危险，扣分
                    score -= (6 - dist) * 40;
                } else {
                    // 自己更大 → 攻击性加分
                    score += (6 - dist) * 15;
                }
            }
        }
        return score;
    }

    /** 曼哈顿距离（考虑地图环绕，取最短） */
    private int manhattan(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x1 - x2);
        int dy = Math.abs(y1 - y2);
        dx = Math.min(dx, MAP_WIDTH - dx);
        dy = Math.min(dy, MAP_HEIGHT - dy);
        return dx + dy;
    }

    private void applyItem(Snake snake, String itemType) {
        switch (itemType) {
            case "speed" -> snake.speedBoost = 5;
            case "shield" -> snake.shield = true;
            case "magnet" -> snake.magnet = 8;
            default -> {
            }
        }
    }

    private void applyMagnetEffect(Snake snake) {
        GridPoint head = snake.body.get(0);
        int range = 8;
        for (int i = foods.size() - 1; i >= 0; i--) {
            Food food = foods.get(i);
            int dist = Math.abs(food.getX() - head.x()) + Math.abs(food.getY() - head.y());
            if (dist <= range && dist > 0) {
                if (Math.abs(food.getX() - head.x()) > Math.abs(food.getY() - head.y())) {
                    food.setX(food.getX() > head.x() ? food.getX() - 1 : food.getX() + 1);
                } else {
                    food.setY(food.getY() > head.y() ? food.getY() - 1 : food.getY() + 1);
                }
                if (food.getX() == head.x() && food.getY() == head.y()) {
                    snake.score += "high".equals(food.getType()) ? 50 : 10;
                    snake.body.add(0, new GridPoint(head.x(), head.y()));
                    snake.speed = Math.min(2.0, snake.speed + 0.02);
                    foods.remove(i);
                }
            }
        }
    }

    private void eliminateSnake(Snake snake, String reason) {
        snake.isAlive = false;
        snake.deathTime = gameTime;
        emitEvent("player_eliminated", Map.of(
            "snakeId", snake.id,
            "nickname", snake.nickname,
            "reason", reason,
            "time", gameTime
        ));

        if (mode == GameMode.SINGLE) {
            endGame();
            return;
        }

        long alive = snakes.values().stream().filter(s -> s.isAlive).count();
        if (alive <= 1) {
            if (alive == 1) {
                snakes.values().stream().filter(s -> s.isAlive).findFirst().ifPresent(s -> s.score += 200);
            }
            endGame();
        }
    }

    private void spawnDeathFoods(Snake snake) {
        for (GridPoint seg : snake.body) {
            if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                foods.add(new Food(seg.x(), seg.y(), ThreadLocalRandom.current().nextDouble() < 0.2 ? "high" : "normal"));
            }
        }
    }

    private List<Obstacle> generateObstacles(int count) {
        List<Obstacle> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Obstacle(
                ThreadLocalRandom.current().nextInt(5, MAP_WIDTH - 5),
                ThreadLocalRandom.current().nextInt(5, MAP_HEIGHT - 5)
            ));
        }
        return list;
    }

    private List<Food> generateFoods(int count) {
        List<Food> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Food(
                ThreadLocalRandom.current().nextInt(1, MAP_WIDTH - 1),
                ThreadLocalRandom.current().nextInt(1, MAP_HEIGHT - 1),
                ThreadLocalRandom.current().nextDouble() < 0.1 ? "high" : "normal"
            ));
        }
        return list;
    }

    private List<Item> generateItems(int count) {
        String[] types = {"speed", "shield", "magnet"};
        List<Item> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Item(
                ThreadLocalRandom.current().nextInt(1, MAP_WIDTH - 1),
                ThreadLocalRandom.current().nextInt(1, MAP_HEIGHT - 1),
                types[ThreadLocalRandom.current().nextInt(types.length)]
            ));
        }
        return list;
    }

    private List<GridPoint> getSpawnPositions(int count) {
        List<GridPoint> positions = new ArrayList<>();
        int margin = 8;
        List<GridPoint> corners = List.of(
            new GridPoint(margin, margin),
            new GridPoint(MAP_WIDTH - margin, margin),
            new GridPoint(margin, MAP_HEIGHT - margin),
            new GridPoint(MAP_WIDTH - margin, MAP_HEIGHT - margin)
        );
        for (int i = 0; i < Math.min(count, corners.size()); i++) {
            positions.add(corners.get(i));
        }
        while (positions.size() < count) {
            positions.add(new GridPoint(
                ThreadLocalRandom.current().nextInt(margin, MAP_WIDTH - margin),
                ThreadLocalRandom.current().nextInt(margin, MAP_HEIGHT - margin)
            ));
        }
        return positions;
    }

    private Direction getInitialDirection(GridPoint pos) {
        int midX = MAP_WIDTH / 2;
        int midY = MAP_HEIGHT / 2;
        if (pos.x() < midX && pos.y() < midY) {
            return Direction.RIGHT;
        }
        if (pos.x() >= midX && pos.y() < midY) {
            return Direction.DOWN;
        }
        if (pos.x() < midX && pos.y() >= midY) {
            return Direction.UP;
        }
        return Direction.LEFT;
    }

    private void spawnItems() {
        synchronized (lock) {
            if (!"playing".equals(status)) {
                return;
            }
            if (items.size() < 5) {
                items.addAll(generateItems(1));
            }
        }
    }

    private void spawnFoods() {
        synchronized (lock) {
            if (!"playing".equals(status)) {
                return;
            }
            if (foods.size() < 25) {
                foods.addAll(generateFoods(2));
            }
        }
    }

    private void broadcastState() {
        if (stateCallback == null) {
            return;
        }
        stateCallback.accept(buildStateSnapshot());
    }

    private void emitEvent(String type, Map<String, Object> data) {
        if (eventCallback == null) {
            return;
        }
        eventCallback.accept(new GameEvent(type, data));
    }

    private GameStateSnapshot buildStateSnapshot() {
        Map<String, SnakeState> snakeStates = new LinkedHashMap<>();
        for (Snake snake : snakes.values()) {
            List<GridPoint> body = new ArrayList<>(snake.body);
            snakeStates.put(snake.id, new SnakeState(
                snake.id,
                body,
                snake.direction.name().toLowerCase(),
                snake.color,
                snake.score,
                snake.kills,
                snake.body.size(),
                snake.isAlive,
                snake.nickname,
                snake.speedBoost,
                snake.shield,
                snake.magnet,
                false
            ));
        }

        List<ScoreEntry> scoreBoard = snakeStates.values().stream()
            .map(s -> new ScoreEntry(
                s.id(),
                s.nickname(),
                s.score(),
                s.kills(),
                s.length(),
                s.isAlive(),
                s.isAlive() ? gameTime : snakes.get(s.id()).deathTime,
                false,
                s.color()
            ))
            .sorted(Comparator.comparingInt(ScoreEntry::score).reversed())
            .toList();

        double totalTime = mode == GameMode.MULTI ? gameDurationSeconds : 0;
        return new GameStateSnapshot(
            snakeStates,
            new ArrayList<>(foods),
            new ArrayList<>(items),
            new ArrayList<>(obstacles),
            scoreBoard,
            gameTime,
            totalTime,
            status,
            MAP_WIDTH,
            MAP_HEIGHT,
            GRID_SIZE
        );
    }

    private void endGame() {
        if ("finished".equals(status)) {
            return;
        }
        engineLog.info("endGame: mode={}, gameTime={}", mode, gameTime);
        status = "finished";
        stopTimers();
        broadcastState();

        List<ScoreEntry> rankings = new ArrayList<>();
        for (Snake snake : snakes.values()) {
            rankings.add(new ScoreEntry(
                snake.id,
                snake.nickname,
                snake.score,
                snake.kills,
                snake.body.size(),
                snake.isAlive,
                snake.isAlive ? gameTime : snake.deathTime,
                false,
                snake.color
            ));
        }
        rankings.sort(Comparator.comparingInt(ScoreEntry::score).reversed());

        if (gameOverCallback != null) {
            gameOverCallback.accept(new GameResult(
                "game_" + System.currentTimeMillis(),
                (int) gameTime,   // 使用实际游戏时长，而非预设时长（单人模式预设为0）
                rankings,
                mode == GameMode.SINGLE ? "single" : "multi"
            ));
        }
    }

    private void stopTimers() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
        }
        if (itemTask != null) {
            itemTask.cancel(false);
            itemTask = null;
        }
        if (foodTask != null) {
            foodTask.cancel(false);
            foodTask = null;
        }
    }

    private boolean samePos(GridPoint a, GridPoint b) {
        return a.x() == b.x() && a.y() == b.y();
    }
}
