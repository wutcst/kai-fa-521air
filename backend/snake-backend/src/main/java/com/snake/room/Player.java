package com.snake.room;

import org.springframework.web.socket.WebSocketSession;

public class Player {
    private final String id;
    private String nickname;
    private String avatar;
    private int level;
    private boolean host;
    private boolean ready;
    private WebSocketSession session;

    public Player(String id, String nickname, String avatar, int level, boolean host) {
        this.id = id;
        this.nickname = nickname;
        this.avatar = avatar;
        this.level = level;
        this.host = host;
        this.ready = host;
    }

    public String getId() {
        return id;
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
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }
}
