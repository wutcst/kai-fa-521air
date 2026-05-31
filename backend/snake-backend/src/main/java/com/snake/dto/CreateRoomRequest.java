package com.snake.dto;

/**
 * 创建房间请求
 */
public class CreateRoomRequest {
    private String name;
    private String gameMode;
    private int maxPlayers;
    private int gameDuration;
    private boolean hasPassword;
    private String password;
    private boolean allowBots;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
}
