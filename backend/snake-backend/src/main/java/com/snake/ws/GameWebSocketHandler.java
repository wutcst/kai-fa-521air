package com.snake.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snake.room.RoomManager;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final RoomManager roomManager;

    public GameWebSocketHandler(ObjectMapper objectMapper, RoomManager roomManager) {
        this.objectMapper = objectMapper;
        this.roomManager = roomManager;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        WsMessage wsMessage = objectMapper.readValue(message.getPayload(), WsMessage.class);
        if (wsMessage == null || wsMessage.type() == null) {
            return;
        }

        switch (wsMessage.type()) {
            case "join_room" ->
                    roomManager.joinRoom(
                            session,
                            objectMapper.convertValue(
                                    wsMessage.data(), RoomManager.JoinRoomRequest.class));
            case "leave_room" ->
                    roomManager.leaveRoom(
                            session,
                            objectMapper.convertValue(
                                    wsMessage.data(), RoomManager.LeaveRoomRequest.class));
            case "ready" ->
                    roomManager.setReady(
                            session,
                            objectMapper.convertValue(
                                    wsMessage.data(), RoomManager.ReadyRequest.class));
            case "start_game" ->
                    roomManager.startGame(
                            session,
                            objectMapper.convertValue(
                                    wsMessage.data(), RoomManager.StartGameRequest.class));
            case "change_direction" ->
                    roomManager.changeDirection(
                            session,
                            objectMapper.convertValue(
                                    wsMessage.data(), RoomManager.DirectionRequest.class));
            case "chat_message" ->
                    roomManager.chat(
                            session,
                            objectMapper.convertValue(
                                    wsMessage.data(), RoomManager.ChatRequest.class));
            case "kick_player" ->
                    roomManager.kickPlayer(
                            session,
                            objectMapper.convertValue(
                                    wsMessage.data(), RoomManager.KickRequest.class));
            case "ping" ->
                    roomManager.sendMessage(
                            session, "pong", java.util.Map.of("time", System.currentTimeMillis()));
            default -> {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        roomManager.handleDisconnect(session);
    }
}
