package com.snake.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 玩家成绩实体
 */
@Entity
@Table(name = "game_player_result", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"game_id", "user_id"})
})
public class GamePlayerResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false)
    private int score = 0;

    @Column(nullable = false)
    private int kills = 0;

    @Column(name = "snake_length", nullable = false)
    private int snakeLength = 0;

    @Column(name = "survival_time", nullable = false)
    private double survivalTime = 0;

    @Column(name = "is_alive", nullable = false)
    private boolean isAlive = true;

    @Column(name = "is_bot", nullable = false)
    private boolean isBot = false;

    @Column(name = "rank", nullable = false)
    private int rank = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }

    public int getSnakeLength() { return snakeLength; }
    public void setSnakeLength(int snakeLength) { this.snakeLength = snakeLength; }

    public double getSurvivalTime() { return survivalTime; }
    public void setSurvivalTime(double survivalTime) { this.survivalTime = survivalTime; }

    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean isAlive) { this.isAlive = isAlive; }

    public boolean isBot() { return isBot; }
    public void setBot(boolean isBot) { this.isBot = isBot; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
