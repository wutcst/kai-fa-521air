package com.snake.room;

public enum RoomStatus {
    WAITING,
    COUNTDOWN,
    PLAYING,
    FINISHED;

    public String toWire() {
        return name().toLowerCase();
    }
}
