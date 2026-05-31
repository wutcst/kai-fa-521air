package com.snake.dto;

import java.util.List;

/**
 * 房间响应
 */
public class RoomResponse {
    private String roomId;
    private String name;
    private String hostId;
    private String hostName;
    private int playerCount;
    private int maxPlayers;
    private String status;
    private boolean hasPassword;
    private int gameDuration;
    private String gameMode;
    private List<PlayerInfo> players;

    public RoomResponse() {}

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isHasPassword() { return hasPassword; }
    public void setHasPassword(boolean hasPassword) { this.hasPassword = hasPassword; }
    public int getGameDuration() { return gameDuration; }
    public void setGameDuration(int gameDuration) { this.gameDuration = gameDuration; }
    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public List<PlayerInfo> getPlayers() { return players; }
    public void setPlayers(List<PlayerInfo> players) { this.players = players; }

    public static class PlayerInfo {
        private String id;
        private String nickname;
        private String avatar;
        private int level;
        private boolean isHost;
        private boolean isReady;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public boolean isHost() { return isHost; }
        public void setHost(boolean isHost) { this.isHost = isHost; }
        public boolean isReady() { return isReady; }
        public void setReady(boolean isReady) { this.isReady = isReady; }
    }
}
