package com.snake.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 房间玩家关联实体
 */
@Entity
@Table(name = "room_player", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"room_id", "user_id"})
})
public class RoomPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, length = 64)
    private String roomId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "is_host", nullable = false)
    private boolean isHost = false;

    @Column(name = "is_ready", nullable = false)
    private boolean isReady = false;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public boolean isHost() { return isHost; }
    public void setHost(boolean isHost) { this.isHost = isHost; }
    public boolean isReady() { return isReady; }
    public void setReady(boolean isReady) { this.isReady = isReady; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
