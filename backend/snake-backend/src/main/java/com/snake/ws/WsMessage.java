package com.snake.ws;

import com.fasterxml.jackson.databind.JsonNode;

public record WsMessage(String type, JsonNode data) {}
