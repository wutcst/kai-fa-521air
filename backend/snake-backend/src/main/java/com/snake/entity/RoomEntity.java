package com.snake.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.springframework.data.domain.Persistable;

/**
 * 房间实体 id 使用 String 类型，与 RoomManager 中的 roomId 一致 实现 Persistable 接口以确保手动 ID 正确触发 persist() 而非
 * merge()
 */
@Entity
@Table(name = "room")
public class RoomEntity implements Persistable<String> {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "host_id", length = 64)
    private String hostId;

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

    /** 标记实体是否为新建（未持久化） true = 首次保存时执行 persist() false = 后续更新时执行 merge() */
    @Transient private boolean isNew = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PostLoad
    private void markNotNew() {
        this.isNew = false;
    }

    // Persistable 接口方法
    @Override
    @Transient
    public boolean isNew() {
        return isNew;
    }

    // Getters and Setters
    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
