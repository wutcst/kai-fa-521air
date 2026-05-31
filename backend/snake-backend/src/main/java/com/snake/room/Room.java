package com.snake.room;

import com.snake.game.GameEngine;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class Room {
    private final Object lock = new Object();
    private final String id;
    private String name;
    private String hostId;
    private int maxPlayers;
    private int gameDuration;
    private String gameMode;
    private boolean hasPassword;
    private String password;
    private boolean allowBots;
    private RoomStatus status = RoomStatus.WAITING;
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private GameEngine engine;
    private LocalDateTime startedAt;
    private ScheduledFuture<?> countdownTask;
    private int countdownSeconds;

    public Room(String id) {
        this.id = id;
    }

    public Object getLock() {
        return lock;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getGameDuration() {
        return gameDuration;
    }

    public void setGameDuration(int gameDuration) {
        this.gameDuration = gameDuration;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public boolean isHasPassword() {
        return hasPassword;
    }

    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAllowBots() {
        return allowBots;
    }

    public void setAllowBots(boolean allowBots) {
        this.allowBots = allowBots;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public Map<String, Player> getPlayers() {
        return players;
    }

    public GameEngine getEngine() {
        return engine;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setEngine(GameEngine engine) {
        this.engine = engine;
    }

    public ScheduledFuture<?> getCountdownTask() {
        return countdownTask;
    }

    public void setCountdownTask(ScheduledFuture<?> countdownTask) {
        this.countdownTask = countdownTask;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public void setCountdownSeconds(int countdownSeconds) {
        this.countdownSeconds = countdownSeconds;
    }
}
