package com.snake.config;

import com.snake.entity.SysUser;
import com.snake.repository.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 数据初始化器 在应用启动时创建或修复默认用户（密码经过 BCrypt 加密） */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createOrUpdate("admin", "123456", "管理员", 10, 9999);
        createOrUpdate("player1", "123456", "玩家一号", 5, 3500);
        createOrUpdate("test", "123456", "测试用户", 1, 100);
    }

    private void createOrUpdate(
            String username, String rawPassword, String nickname, int level, long totalScore) {
        SysUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setNickname(nickname);
            user.setAvatar("");
            user.setLevel(level);
            user.setTotalScore(totalScore);
            user.setTotalGames(0);
            user.setWins(0);
            user.setStatus(1);
            userRepository.save(user);
            log.info("Created default user: {}/{}", username, rawPassword);
        } else if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            // 密码不匹配时修复（例如 data.sql 中的哈希不准确）
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            log.info("Fixed password for user: {}", username);
        }
    }
}
