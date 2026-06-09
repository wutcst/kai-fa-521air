# Snake Backend 测试文档

## 概述

| 项目 | 值 |
|---|---|
| 框架 | Spring Boot 4.0.6 / Java 21 |
| 单元测试 | JUnit 5 + Mockito (MockMvc / @ExtendWith) |
| 集成测试 | JUnit 5 + @SpringBootTest(RANDOM_PORT) + RestTemplate |
| 构建工具 | Maven Surefire (groups 标签分类) |
| 测试总数 | **19 个集成测试 + 27 个单元测试（含 1 占位）= 46 个测试** |

### 运行方式

```bash
# 运行所有测试
.\mvnw.cmd test

# 仅运行单元测试（排除集成测试）
.\mvnw.cmd test -DexcludedGroups=integration

# 仅运行集成测试（需要 docker-compose 已启动）
.\mvnw.cmd test -Dgroups=integration
```

---

## 一、单元测试（Unit Tests）

单元测试使用 Mockito 模拟依赖，不启动 Spring 上下文，速度快且无需外部服务。

### 1.1 AuthControllerTest

| 文件 | `src/test/java/com/snake/controller/AuthControllerTest.java` |
|---|---|
| 技术栈 | `MockMvc` + `MockitoExtension` |
| 模拟对象 | `SysUserRepository`, `PasswordEncoder`, `JwtUtil` |
| 测试数 | **10 个** |

**测试列表：**

| 序号 | 测试方法 | 验证内容 |
|---|---|---|
| 1 | `login_WithValidCredentials_ShouldReturnToken` | 用户名密码正确 → 200 + 返回 token + 用户信息 |
| 2 | `login_WithInvalidUsername_ShouldReturn401` | 用户名不存在 → 401 + "用户名或密码错误" |
| 3 | `login_WithNullCredentials_ShouldReturn400` | 用户名密码为空 → 400 + "用户名和密码不能为空" |
| 4 | `login_WithDisabledUser_ShouldReturn403` | 账号被禁用(status=0) → 403 + "账号已被禁用" |
| 5 | `login_WithWrongPassword_ShouldReturn401` | 密码错误 → 401 + "用户名或密码错误" |
| 6 | `register_WithValidData_ShouldReturn201` | 注册信息合法 → 201 + 返回 token + 用户信息 |
| 7 | `register_WithExistingUsername_ShouldReturn400` | 用户名已存在 → 400 + "用户名已存在" |
| 8 | `register_WithShortPassword_ShouldReturn400` | 密码少于6位 → 400 + "密码长度不能少于6位" |
| 9 | `register_WithShortUsername_ShouldReturn400` | 用户名少于3字符 → 400 + "用户名长度须在3-50个字符之间" |
| 10 | `register_WithNullUsernameAndPassword_ShouldReturn400` | 注册请求体为空 → 400 + "用户名和密码不能为空" |

### 1.2 GameEngineTest

| 文件 | `src/test/java/com/snake/game/GameEngineTest.java` |
|---|---|
| 技术栈 | 纯 JUnit 5（无 Mockito） |
| 测试数 | **12 个** |

**测试列表：**

| 序号 | 测试方法 | 验证内容 |
|---|---|---|
| 1 | `directionOpposite_ShouldReturnCorrectDirection` | 方向取反（UP→DOWN, LEFT→RIGHT 等） |
| 2 | `directionFrom_ShouldReturnCorrectDirection` | 字符串解析方向（大小写不敏感） |
| 3 | `directionFrom_InvalidString_ShouldReturnNull` | 无效字符串返回 null |
| 4 | `directionDxDy_ShouldMatchExpected` | 方向对应的坐标增量 |
| 5 | `gameModeFrom_ShouldParseCorrectly` | 游戏模式字符串解析 |
| 6 | `constructor_ShouldCreateSnakesAndReceiveState` | 引擎初始化后能收到状态回调 |
| 7 | `setDirection_WithOpposite_ShouldBeIgnored` | 设置相反方向被忽略 |
| 8 | `setDirection_WithNullPlayerId_ShouldNotThrow` | null 玩家 ID 不抛异常 |
| 9 | `eliminatePlayer_InSingleMode_ShouldEndGame` | 单人模式淘汰唯一玩家 → 游戏结束 |
| 10 | `eliminatePlayer_InMultiMode_ShouldEndWhenAllDead` | 多人模式淘汰到最后1人 → 游戏结束 |
| 11 | `stop_ShouldSetStatusToFinished` | 停止引擎（多次调用不抛异常） |
| 12 | `start_ShouldReceiveStateCallbacks` | 引擎启动后收到状态回调（重复 start 是空操作） |
| 13 | `gridPoint_ShouldStoreCoordinates` | GridPoint 坐标存储 |
| 14 | `snakeState_ShouldStoreAllFields` | SnakeState 数据类全部字段 |
| 15 | `scoreEntry_ShouldStoreAllFields` | ScoreEntry 数据类全部字段 |
| 16 | `food_ShouldStoreAndAllowUpdate` | Food 数据类读写 |
| 17 | `item_ShouldStoreAndAllowUpdate` | Item 数据类读写 |

> 注：涉及线程/定时器的测试使用 `Thread.sleep()` 等待异步回调。

### 1.3 GameServiceTest

| 文件 | `src/test/java/com/snake/service/GameServiceTest.java` |
|---|---|
| 技术栈 | `MockitoExtension` |
| 模拟对象 | `GameRepository`, `GamePlayerResultRepository`, `SysUserRepository` |
| 测试数 | **6 个** |

**测试列表：**

| 序号 | 测试方法 | 验证内容 |
|---|---|---|
| 1 | `saveGameResult_ShouldSaveGameAndPlayerResults` | 保存游戏结果 → 1 条 game + N 条 player_result + 更新用户统计（Bot 不更新） |
| 2 | `getGameHistory_ShouldReturnPaginatedResults` | 分页查询游戏历史 → 返回 total/page/size/list |
| 3 | `getGameHistory_WithModeFilter_ShouldCallCorrectRepository` | 按模式过滤 → 调用 `findByGameMode` |
| 4 | `getGameDetail_ShouldReturnGameWithPlayers` | 查询游戏详情 → 返回 game 信息 + 玩家列表（含昵称） |
| 5 | `getGameDetail_WithNonExistentGame_ShouldReturnNull` | 不存在的游戏 → 返回 null |
| 6 | `getPlayerStats_ShouldReturnCorrectStats` | 查询玩家统计 → 累计场次/总分/总击杀/胜场/最高分 |

### 1.4 JwtUtilTest

| 文件 | `src/test/java/com/snake/util/JwtUtilTest.java` |
|---|---|
| 技术栈 | 纯 JUnit 5 |
| 测试数 | **10 个** |

**测试列表：**

| 序号 | 测试方法 | 验证内容 |
|---|---|---|
| 1 | `generateToken_ShouldReturnValidToken` | 生成 token → 非空且可验证 |
| 2 | `generateToken_ShouldEncodeUserId` | token 能正确解码 userId |
| 3 | `generateToken_ShouldEncodeUsername` | token 能正确解码 username |
| 4 | `validateToken_WithInvalidToken_ShouldReturnFalse` | 无效 token → 验证失败 |
| 5 | `validateToken_WithEmptyToken_ShouldReturnFalse` | 空字符串 token → 验证失败 |
| 6 | `validateToken_WithNullToken_ShouldReturnFalse` | null token → 验证失败 |
| 7 | `validateToken_WithTamperedToken_ShouldReturnFalse` | 篡改 token → 验证失败 |
| 8 | `getUserIdFromToken_WithMultipleUsers_ShouldReturnCorrectId` | 多用户 token 解码 userId 正确 |
| 9 | `getUsernameFromToken_WithMultipleUsers_ShouldReturnCorrectUsername` | 多用户 token 解码 username 正确 |
| 10 | `token_ShouldNotBeExpired` | 24h 有效期 token 立即验证有效 |
| 11 | `generateToken_WithSameData_ShouldContainSameClaims` | 相同数据生成的 token 解码后 claims 一致 |

### 1.5 SnakeBackendApplicationTests

| 文件 | `src/test/java/com/snake/SnakeBackendApplicationTests.java` |
|---|---|
| 测试数 | **1 个**（占位测试） |

> 占位测试，验证测试框架正常加载。不加载 Spring 上下文（因为本地开发无 MySQL/Redis）。

---

## 二、集成测试（Integration Tests）

集成测试启动完整 Spring Boot 应用（`RANDOM_PORT`），连接 Docker MySQL（3307）和 Docker Redis（6379），通过 `RestTemplate` 发送 HTTP 请求验证 API 端点。

### 前置条件

```bash
# 在项目根目录启动 Docker 服务
cd D:\RjSnake2\kai-fa-521air
docker-compose up -d
```

### 2.1 基类 BaseIntegrationTest

| 文件 | `src/test/java/com/snake/integration/BaseIntegrationTest.java` |
|---|---|
| 注解 | `@SpringBootTest(webEnvironment=RANDOM_PORT)` |
| Profile | `@ActiveProfiles("integration")` |
| 标签 | `@Tag("integration")` |

**自动注入：**
- `SysUserRepository` / `GameRepository` / `GamePlayerResultRepository`
- `RoomRepository` / `RoomPlayerRepository`
- `JdbcTemplate`（用于清理数据）
- `RestTemplate`（手动创建，TestRestTemplate 在 Spring Boot 4.0 不存在）

**自定义错误处理器：**
`RestTemplate.setErrorHandler` 覆盖为仅对 5xx 抛出异常，4xx 响应正常返回，使测试能断言错误状态码。

**清理机制：**
`@AfterEach cleanUpTestData()` 按外键约束顺序删除所有表数据：
```
game_player_result → game → room_player → room → sys_user
```

**辅助方法：**
| 方法 | 用途 |
|---|---|
| `post(path, body, responseType)` | POST JSON 请求 |
| `getWithToken(path, token, responseType)` | GET 请求 + Bearer Token |

**注意：** `restTemplate.postForEntity()` 在 POST + 401 响应时 body 为 null（Spring Boot 4.0 行为），相关测试只断言状态码。

### 2.2 AuthIntegrationTest

| 文件 | `src/test/java/com/snake/integration/AuthIntegrationTest.java` |
|---|---|
| 测试数 | **9 个** |

**测试列表：**

| 序号 | 测试方法 | 请求 | 预期 |
|---|---|---|---|
| 1 | `register_ShouldCreateUserAndReturnToken` | POST /api/auth/register | 201 + token + user.username |
| 2 | `register_WithExistingUsername_ShouldReturn400` | POST /api/auth/register (重复用户名) | 400 + "用户名已存在" |
| 3 | `register_WithShortPassword_ShouldReturn400` | POST /api/auth/register (密码5位) | 400 + "密码长度不能少于6位" |
| 4 | `login_WithCorrectCredentials_ShouldReturnToken` | POST /api/auth/login (正确密码) | 200 + token |
| 5 | `login_WithWrongPassword_ShouldReturn401` | POST /api/auth/login (错误密码) | 401 |
| 6 | `login_WithNonExistentUser_ShouldReturn401` | POST /api/auth/login (不存在用户) | 401 |
| 7 | `login_WithEmptyCredentials_ShouldReturn400` | POST /api/auth/login (空凭据) | 400 + "用户名和密码不能为空" |
| 8 | `getMe_WithValidToken_ShouldReturnUserInfo` | GET /api/auth/me + Bearer token | 200 + username |
| 9 | `getMe_WithoutToken_ShouldReturn401` | GET /api/auth/me (无 token) | 401 + "未登录" |

**完整流程测试：**
注册 → 登录 → 获取用户信息，覆盖完整的 JWT 认证生命周期。

### 2.3 GameIntegrationTest

| 文件 | `src/test/java/com/snake/integration/GameIntegrationTest.java` |
|---|---|
| 测试数 | **5 个** |

**前置数据（`@BeforeEach`）：**
- 1 条 game 记录（mode=single, status=finished）
- 1 条 player_result 记录（userId=999999, score=500）

**测试列表：**

| 序号 | 测试方法 | 请求 | 预期 |
|---|---|---|---|
| 1 | `getGameHistory_ShouldReturnGames` | GET /api/games | 200 + list + total≥1 |
| 2 | `getGameHistory_WithModeFilter_ShouldReturnFiltered` | GET /api/games?mode=single | 200 + total≥1 |
| 3 | `getGameDetail_WithExistingGame_ShouldReturnDetail` | GET /api/games/{id} | 200 + id + players |
| 4 | `getGameDetail_WithNonExistentGame_ShouldReturn404` | GET /api/games/99999 | 404 + "游戏记录不存在" |
| 5 | `getPlayerStats_ShouldReturnStats` | GET /api/games/player/999999/stats | 200 + userId + totalGames≥1 |

### 2.4 RankingIntegrationTest

| 文件 | `src/test/java/com/snake/integration/RankingIntegrationTest.java` |
|---|---|
| 测试数 | **5 个** |

**前置数据（`@BeforeEach`）：**
- 1 个测试用户（totalScore=1000, wins=3, level=5）
- 1 个多人游戏（mode=multi）+ 1 条 player_result（score=500）
- 1 个单人游戏（mode=single）+ 1 条 player_result（score=300）

**测试列表：**

| 序号 | 测试方法 | 请求 | 预期 |
|---|---|---|---|
| 1 | `getOverallRanking_ShouldReturnList` | GET /api/ranking | 200 + list |
| 2 | `getMultiRanking_ShouldReturnList` | GET /api/ranking?mode=multi | 200 + list |
| 3 | `getSingleRanking_ShouldReturnList` | GET /api/ranking?mode=single | 200 + list |
| 4 | `getUserRank_ShouldReturnUserRanking` | GET /api/ranking/user/{id} | 200 + overallRank + nickname |
| 5 | `getRanking_WithPagination_ShouldRespectPageSize` | GET /api/ranking?page=1&size=5 | 200 + page=1 + size=5 |

---

## 三、测试配置

### 单元测试配置（默认 profile）

| 文件 | `src/test/resources/application.properties` |
|---|---|
| 数据库 | 禁用（`spring.sql.init.mode=never`, `ddl-auto=validate`） |
| Redis | 禁用（`spring.data.redis.host=` 留空） |
| JWT 密钥 | `test-secret-key-that-is-at-least-256-bits-long-for-hs512-algorithm!!` |

单元测试不加载 Spring 上下文（仅 `@ExtendWith(MockitoExtension.class)`），因此数据库和 Redis 配置仅用于占位，不被实际使用。

### 集成测试配置（integration profile）

| 文件 | `src/test/resources/application-integration.properties` |
|---|---|
| 数据库 | MySQL `127.0.0.1:3307/snake_game_test`（Docker） |
| Redis | `127.0.0.1:6379` database=1 |
| JPA | `ddl-auto=update`（Hibernate 自动建表） |
| JWT 密钥 | `snake-game-jwt-secret-key-must-be-at-least-256-bits-long-for-hs512`（与 SecurityConfig 硬编码一致） |

---

## 四、覆盖情况

| 模块 | 单元测试 | 集成测试 | 合计 |
|---|---|---|---|
| AuthController (认证 API) | 10 | 9 | 19 |
| GameEngine (游戏引擎) | 17 | — | 17 |
| GameService (游戏服务) | 6 | — | 6 |
| JwtUtil (JWT 工具) | 11 | — | 11 |
| 游戏 API | — | 5 | 5 |
| 排行榜 API | — | 5 | 5 |
| 占位测试 | 1 | — | 1 |
| **总计** | **45** | **19** | **64** |

> 注：单元测试使用 `@ExtendWith(MockitoExtension.class)` 时，Mockito 会严格验证所有 mock 的交互。当测试方法定义了未使用的 mock 桩时会抛出 `UnnecessaryStubbingException`，这是设计预期的行为，每个测试方法只定义其所需的 mock 桩。

---

## 五、常见问题

### 5.1 集成测试 401 POST body 为 null

Spring Boot 4.0 的 `RestTemplate` 在 POST 请求返回 401 时 body 为 null（GET 请求正常）。这是框架行为，相关测试(`login_WithWrongPassword_ShouldReturn401`, `login_WithNonExistentUser_ShouldReturn401`) 只断言状态码 401，不验证 body 内容。

### 5.2 surefire 标签（groups）过滤

- 集成测试类标注 `@Tag("integration")`
- 运行命令 `-Dgroups=integration` 仅执行集成测试
- 排除命令 `-DexcludedGroups=integration` 仅执行单元测试

### 5.3 Mockito UnnecessaryStubbingException

在 `@ExtendWith(MockitoExtension.class)` 中，如果在 `@BeforeEach` 中定义了 mock 桩但在所有测试方法中都未使用，Mockito 会报错。解决方法：不在 `@BeforeEach` 中定义桩，只在每个测试方法中定义所需桩。

---

*文档生成时间：2026-06-09*
*最后更新：2026-06-09*
