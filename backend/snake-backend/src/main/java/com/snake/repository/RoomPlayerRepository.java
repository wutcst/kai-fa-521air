package com.snake.repository;

import com.snake.entity.RoomPlayer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 房间玩家关联数据访问接口 */
@Repository
public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, Long> {

    List<RoomPlayer> findByRoomId(String roomId);

    Optional<RoomPlayer> findByRoomIdAndUserId(String roomId, String userId);

    long countByRoomId(String roomId);

    void deleteByRoomIdAndUserId(String roomId, String userId);

    void deleteByRoomId(String roomId);
}
