package com.snake.repository;

import com.snake.entity.RoomEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 房间数据访问接口 */
@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, String> {

    List<RoomEntity> findByStatusOrderByCreatedAtDesc(String status);

    List<RoomEntity> findByGameModeAndStatus(String gameMode, String status);

    long countByStatus(String status);
}
