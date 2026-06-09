package com.snake.integration;

import com.snake.repository.GamePlayerResultRepository;
import com.snake.repository.GameRepository;
import com.snake.repository.RoomPlayerRepository;
import com.snake.repository.RoomRepository;
import com.snake.repository.SysUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * 集成测试基类
 * - 连接 Docker MySQL（3307）和 Docker Redis（6379）
 * - 使用 RANDOM_PORT 启动完整 Spring 上下文
 * - 每个测试类运行后清理测试数据
 *
 * 前置条件：docker-compose 已启动（MySQL + Redis）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Tag("integration")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected SysUserRepository userRepository;

    @Autowired
    protected GameRepository gameRepository;

    @Autowired
    protected GamePlayerResultRepository playerResultRepository;

    @Autowired
    protected RoomRepository roomRepository;

    @Autowired
    protected RoomPlayerRepository roomPlayerRepository;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /** 手动创建的 RestTemplate（TestRestTemplate 在 Spring Boot 4.0 中不可用） */
    protected final RestTemplate restTemplate;

    public BaseIntegrationTest() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
        // 不抛出4xx异常，让测试用例自行断言状态码
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatusCode statusCode) {
                return statusCode.is5xxServerError();
            }
        });
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * POST 请求辅助方法：发送 JSON body，返回 ResponseEntity
     * 使用 HttpEntity（而非 RequestEntity），确保 4xx 响应 body 正常返回
     */
    protected <T> ResponseEntity<T> post(String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(baseUrl() + path, HttpMethod.POST, entity, responseType);
    }

    /**
     * GET 请求辅助方法：携带 Bearer token
     */
    protected <T> ResponseEntity<T> getWithToken(String path, String token, Class<T> responseType) {
        RequestEntity<Void> request = RequestEntity
                .get(URI.create(baseUrl() + path))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        return restTemplate.exchange(request, responseType);
    }

    /**
     * 每个测试方法后清理所有测试数据
     * 注意：外键约束要求按特定顺序删除
     */
    @AfterEach
    protected void cleanUpTestData() {
        jdbcTemplate.execute("DELETE FROM game_player_result");
        jdbcTemplate.execute("DELETE FROM game");
        jdbcTemplate.execute("DELETE FROM room_player");
        jdbcTemplate.execute("DELETE FROM room");
        jdbcTemplate.execute("DELETE FROM sys_user");
    }
}
