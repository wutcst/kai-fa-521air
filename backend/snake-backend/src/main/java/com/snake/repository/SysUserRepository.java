package com.snake.repository;

import com.snake.entity.SysUser;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** 用户数据访问接口 */
@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    boolean existsByUsername(String username);

    /** 按累计总分降序分页查询（总榜） */
    Page<SysUser> findAllByStatusOrderByTotalScoreDesc(int status, Pageable pageable);

    /** 查询总分大于指定值的用户数（用于计算排名） */
    @Query("SELECT COUNT(u) FROM SysUser u WHERE u.status = 1 AND u.totalScore > :score")
    long countByTotalScoreGreaterThan(long score);

    /** 查询胜场大于指定值的用户数（用于计算胜场排名） */
    @Query("SELECT COUNT(u) FROM SysUser u WHERE u.status = 1 AND u.wins > :wins")
    long countByWinsGreaterThan(int wins);
}
