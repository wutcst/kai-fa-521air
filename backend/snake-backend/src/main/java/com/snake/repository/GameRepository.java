package com.snake.repository;

import com.snake.entity.GameEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 游戏会话数据访问接口
 */
@Repository
public interface GameRepository extends JpaRepository<GameEntity, Long> {

    List<GameEntity> findByRoomIdOrderByCreatedAtDesc(String roomId);

    List<GameEntity> findByGameModeOrderByCreatedAtDesc(String gameMode);

    Page<GameEntity> findByGameMode(String gameMode, Pageable pageable);
}
