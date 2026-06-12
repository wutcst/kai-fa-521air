package com.snake.repository;

import com.snake.entity.GamePlayerResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 玩家成绩数据访问接口 */
@Repository
public interface GamePlayerResultRepository extends JpaRepository<GamePlayerResult, Long> {

    List<GamePlayerResult> findByGameIdOrderByScoreDesc(Long gameId);

    List<GamePlayerResult> findByUserIdOrderByCreatedAtDesc(String userId);

    List<GamePlayerResult> findByUserId(String userId);

    List<GamePlayerResult> findByGameIdAndUserId(Long gameId, String userId);
}
