package com.snake.repository;

import com.snake.entity.GamePlayerResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 玩家成绩数据访问接口
 */
@Repository
public interface GamePlayerResultRepository extends JpaRepository<GamePlayerResult, Long> {

    List<GamePlayerResult> findByGameIdOrderByScoreDesc(Long gameId);

    List<GamePlayerResult> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GamePlayerResult> findByGameIdAndUserId(Long gameId, Long userId);
}
