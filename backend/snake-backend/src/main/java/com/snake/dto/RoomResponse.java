package com.snake.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 房间响应 */
@Schema(description = "房间详情响应")
public class RoomResponse {
    @Schema(description = "房间ID", example = "room_1718000000000_1")
    private String roomId;

    @Schema(description = "房间名称", example = "高手快来")
    private String name;

    @Schema(description = "房主用户ID", example = "1")
    private String hostId;

    @Schema(description = "房主昵称", example = "Admin")
    private String hostName;

    @Schema(description = "当前玩家数", example = "3")
    private int playerCount;

    @Schema(description = "最大玩家数", example = "6")
    private int maxPlayers;

    @Schema(
            description = "房间状态：waiting=等待中 / full=已满 / playing=游戏中 / finished=已结束",
            example = "waiting")
    private String status;

    @Schema(description = "是否有密码", example = "false")
    private boolean hasPassword;

    @Schema(description = "游戏时长（秒）", example = "300")
    private int gameDuration;

    @Schema(description = "游戏模式：multi=多人 / single=单人", example = "multi")
    private String gameMode;

    @Schema(description = "玩家列表")
    private List<PlayerInfo> players;

    public RoomResponse() {}

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
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

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHasPassword() {
        return hasPassword;
    }

    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
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

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfo> players) {
        this.players = players;
    }

    @Schema(description = "玩家信息")
    public static class PlayerInfo {
        @Schema(description = "玩家用户ID", example = "1")
        private String id;

        @Schema(description = "玩家昵称", example = "Admin")
        private String nickname;

        @Schema(description = "头像URL", example = "https://example.com/avatar.png")
        private String avatar;

        @Schema(description = "等级", example = "5")
        private int level;

        @Schema(description = "是否为房主", example = "true")
        private boolean isHost;

        @Schema(description = "是否已准备", example = "false")
        private boolean isReady;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public boolean isHost() {
            return isHost;
        }

        public void setHost(boolean isHost) {
            this.isHost = isHost;
        }

        public boolean isReady() {
            return isReady;
        }

        public void setReady(boolean isReady) {
            this.isReady = isReady;
        }
    }
}
