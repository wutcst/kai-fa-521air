package com.snake.room;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomManager roomManager;

    public RoomController(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @GetMapping("/list")
    public RoomManager.RoomListResponse listRooms(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "10") int size,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "mode", required = false) String mode
    ) {
        return roomManager.listRooms(new RoomManager.RoomQuery(page, size, status, keyword, mode));
    }

    @GetMapping("/online-count")
    public java.util.Map<String, Object> onlineCount() {
        return java.util.Map.of("count", roomManager.getOnlineCount());
    }
}
