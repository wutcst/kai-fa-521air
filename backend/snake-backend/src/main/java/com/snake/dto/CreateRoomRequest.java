package com.snake.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 创建房间请求 */
@Schema(description = "创建房间请求")
public class CreateRoomRequest {
    @Schema(description = "房间名称（可选，默认为'<昵称>的房间'）", example = "高手快来")
    private String name;

    @Schema(description = "游戏模式：multi=多人 / single=单人", example = "multi")
    private String gameMode;

    @Schema(description = "最大玩家数（可选，默认为6，单人模式为1）", example = "6")
    private int maxPlayers;

    @Schema(description = "游戏时长（秒，可选，默认为300）", example = "300")
    private int gameDuration;

    @Schema(description = "是否设置密码", example = "false")
    private boolean hasPassword;

    @Schema(description = "房间密码（hasPassword=true 时必填）", example = "8888")
    private String password;

    @Schema(description = "是否允许机器人加入", example = "false")
    private boolean allowBots;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
