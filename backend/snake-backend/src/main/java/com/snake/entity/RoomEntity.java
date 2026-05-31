package com.snake.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 房间实体
 */
@Entity
@Table(name = "room")
public class RoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "host_id")
    private Long hostId;

    @Column(name = "game_mode", nullable = false, length = 10)
    private String gameMode = "multi";

    @Column(name = "max_players", nullable = false)
    private int maxPlayers = 6;

    @Column(name = "game_duration", nullable = false)
    private int gameDuration = 300;

    @Column(name = "has_password", nullable = false)
    private boolean hasPassword = false;

    @Column(length = 50)
    private String password;

    @Column(name = "allow_bots", nullable = false)
    private boolean allowBots = false;

    @Column(nullable = false, length = 10)
    private String status = "waiting";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }

    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public int getGameDuration() { return gameDuration; }
    public void setGameDuration(int gameDuration) { this.gameDuration = gameDuration; }

    public boolean isHasPassword() { return hasPassword; }
    public void setHasPassword(boolean hasPassword) { this.hasPassword = hasPassword; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isAllowBots() { return allowBots; }
    public void setAllowBots(boolean allowBots) { this.allowBots = allowBots; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
