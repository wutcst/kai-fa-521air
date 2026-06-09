# Snake Game API 文档

## 基本信息

- **基础URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 认证方式

注册或登录后获取 JWT token，在后续请求的 HTTP Header 中携带：

```
Authorization: Bearer <your_token>
```

---

## 目录

| 分组 | Tag | 说明 |
|---|---|---|
| 认证管理 | `01-认证管理` | 注册、登录、获取用户信息 |
| 游戏记录 | `02-游戏记录` | 游戏历史查询、详情、玩家统计 |
| 排行榜 | `03-排行榜` | 总榜、分模式排行、用户排名 |
| 房间管理 | `04-房间管理` | 房间列表、创建、加入、退出 |
| 匹配管理 | `05-匹配管理` | 快速匹配队列管理 |

---

## 01-认证管理

### POST /api/auth/register
用户注册

**Request Body:**
```json
{
  "username": "newplayer",      // 必填，3-50个字符
  "password": "password123",    // 必填，至少6位
  "nickname": "NewPlayer"       // 可选，默认为用户名
}
```

**Response 201:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "user": {
    "id": 2,
    "username": "newplayer",
    "nickname": "NewPlayer",
    "avatar": "",
    "level": 1,
    "totalScore": 0
  }
}
```

**Error 400:**
```json
{ "message": "用户名已存在" }
```
```json
{ "message": "用户名长度须在3-50个字符之间" }
```
```json
{ "message": "密码长度不能少于6位" }
```

---

### POST /api/auth/login
用户登录

**Request Body:**
```json
{
  "username": "admin",     // 必填
  "password": "123456"     // 必填
}
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "user": {
    "id": 1,
    "username": "admin",
    "nickname": "Admin",
    "avatar": "https://example.com/avatar.png",
    "level": 5,
    "totalScore": 15000
  }
}
```

**Error 400:**
```json
{ "message": "用户名和密码不能为空" }
```

**Error 401:**
```json
{ "message": "用户名或密码错误" }
```

**Error 403:**
```json
{ "message": "账号已被禁用" }
```

---

### GET /api/auth/me
获取当前用户信息。需要 JWT token。

**Response 200:**
```json
{
  "id": 1,
  "username": "admin",
  "nickname": "Admin",
  "avatar": "https://example.com/avatar.png",
  "level": 5,
  "totalScore": 15000
}
```

**Error 401:**
```json
{ "message": "未登录" }
```
```json
{ "message": "用户不存在" }
```

---

### POST /api/auth/logout
退出登录。JWT 无状态，仅返回成功消息，客户端需自行删除 token。不需要 token。

**Response 200:**
```json
{ "message": "退出成功" }
```

---

## 02-游戏记录

### GET /api/games
获取游戏历史列表（分页）

**Query Parameters:**
| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| page | int | 1 | 页码，从1开始 |
| size | int | 20 | 每页条数，最大100 |
| mode | string | - | 可选，游戏模式筛选：multi / single |

**Response 200:**
```json
{
  "list": [],
  "total": 0,
  "page": 1,
  "size": 20,
  "totalPages": 0
}
```

---

### GET /api/games/{gameId}
获取游戏详情

**Path Parameters:**
| 参数 | 类型 | 说明 |
|---|---|---|
| gameId | long | 游戏ID |

**Response 200:** 游戏详情对象

**Error 404:**
```json
{ "message": "游戏记录不存在" }
```

---

### GET /api/games/my-history
获取当前用户的游戏历史。需要 JWT token。

**Query Parameters:** 同 `GET /api/games`

**Response 200:** 游戏历史列表（分页结构）

**Error 401:**
```json
{ "message": "未登录" }
```

---

### GET /api/games/my-stats
获取当前用户的统计概览。需要 JWT token。

**Response 200:**
```json
{
  "totalGames": 42,
  "totalScore": 15000,
  "wins": 25,
  "level": 5
}
```

**Error 401:**
```json
{ "message": "未登录" }
```

---

### GET /api/games/player/{userId}/history
获取指定玩家的游戏历史（公开）

**Path Parameters:**
| 参数 | 类型 | 说明 |
|---|---|---|
| userId | string | 用户ID |

**Query Parameters:** 同 `GET /api/games`

**Response 200:** 游戏历史列表（分页结构）

---

### GET /api/games/player/{userId}/stats
获取指定玩家的统计概览（公开）

**Path Parameters:**
| 参数 | 类型 | 说明 |
|---|---|---|
| userId | string | 用户ID |

**Response 200:** 玩家统计概览对象

---

## 03-排行榜

### GET /api/ranking
获取排行榜列表

**Query Parameters:**
| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| mode | string | overall | 模式：overall(总榜) / multi(多人) / single(单人) |
| page | int | 1 | 页码 |
| size | int | 20 | 每页条数，最大100 |

**Response 200:**
```json
{
  "list": [
    {
      "userId": "1",
      "nickname": "Admin",
      "totalScore": 15000,
      "overallRank": 1,
      "gamesPlayed": 42,
      "wins": 25
    }
  ],
  "total": 10,
  "page": 1,
  "size": 20,
  "totalPages": 1
}
```

---

### GET /api/ranking/my-rank
获取当前用户的排名信息。需要 JWT token。

**Response 200:**
```json
{
  "userId": "1",
  "nickname": "Admin",
  "totalScore": 15000,
  "overallRank": 1,
  "gamesPlayed": 42,
  "wins": 25
}
```

**Error 401:**
```json
{ "message": "未登录" }
```

---

### GET /api/ranking/user/{userId}
获取指定用户的排名信息（公开）

**Path Parameters:**
| 参数 | 类型 | 说明 |
|---|---|---|
| userId | string | 用户ID |

**Response 200:** 用户排名信息对象

---

## 04-房间管理

### GET /api/rooms/list
获取房间列表（分页）

**Query Parameters:**
| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| page | int | 1 | 页码 |
| size | int | 100 | 每页条数 |
| status | string | - | 筛选状态：waiting / playing / finished |
| keyword | string | - | 按房间名称关键字搜索 |
| mode | string | - | 按游戏模式筛选：multi / single |

**Response 200:**
```json
{
  "list": [
    {
      "id": "room_1718000000000_1",
      "name": "高手快来",
      "hostId": "1",
      "hostName": "Admin",
      "playerCount": 3,
      "maxPlayers": 6,
      "status": "waiting",
      "hasPassword": false,
      "gameDuration": 300,
      "gameMode": "multi",
      "allowBots": false
    }
  ],
  "total": 5,
  "page": 1,
  "size": 100
}
```

---

### GET /api/rooms/online-count
获取在线人数

**Response 200:**
```json
{ "count": 42 }
```

---

### POST /api/rooms
创建房间。需要 JWT token。

**Request Body:**
```json
{
  "name": "高手快来",           // 可选，默认为"<昵称>的房间"
  "gameMode": "multi",         // 可选，multi(多人) / single(单人)
  "maxPlayers": 6,             // 可选，默认6，单人模式固定为1
  "gameDuration": 300,         // 可选，游戏时长（秒），默认300，单人模式为0
  "hasPassword": false,        // 可选，是否设置密码
  "password": "8888",          // hasPassword=true时必填
  "allowBots": false           // 可选，是否允许机器人加入
}
```

**Response 200:**
```json
{
  "roomId": "room_1718000000000_1",
  "name": "高手快来",
  "gameMode": "multi",
  "maxPlayers": 6,
  "gameDuration": 300,
  "hasPassword": false,
  "allowBots": false,
  "hostId": "1",
  "hostName": "Admin",
  "playerCount": 1,
  "status": "waiting"
}
```

**Error 401:**
```json
{ "message": "未登录" }
```
```json
{ "message": "用户不存在" }
```

---

### POST /api/rooms/{roomId}/join
加入房间。需要 JWT token。

**Path Parameters:**
| 参数 | 类型 | 说明 |
|---|---|---|
| roomId | string | 房间ID |

**Request Body（有密码的房间必填）:**
```json
{ "password": "8888" }
```

**Response 200:**
```json
{ "roomId": "room_1718000000000_1", "success": true }
```

**Error 400:**
```json
{ "message": "房间已满" }
```
```json
{ "message": "游戏已开始，无法加入" }
```
```json
{ "message": "需要房间密码" }
```
```json
{ "message": "密码错误" }
```

**Error 401:**
```json
{ "message": "未登录" }
```

**Error 404:**
```json
{ "message": "房间不存在" }
```

---

### POST /api/rooms/{roomId}/leave
退出房间。需要 JWT token。

**Path Parameters:**
| 参数 | 类型 | 说明 |
|---|---|---|
| roomId | string | 房间ID |

**Response 200:**
```json
{ "success": true }
```

**Error 401:**
```json
{ "message": "未登录" }
```

---

### PUT /api/rooms/{roomId}
更新房间设置。需要 JWT token。

**Path Parameters:**
| 参数 | 类型 | 说明 |
|---|---|---|
| roomId | string | 房间ID |

**Request Body（按需传入）:**
```json
{
  "name": "新房间名",
  "maxPlayers": 8,
  "gameDuration": 180,
  "hasPassword": true,
  "password": "1234",
  "allowBots": true
}
```

**Response 200:**
```json
{ "success": true }
```

**Error 401:**
```json
{ "message": "未登录" }
```

**Error 404:**
```json
{ "message": "房间不存在" }
```

---

### GET /api/rooms/{roomId}
获取房间详情

**Path Parameters:**
| 参数 | 类型 | 说明 |
|---|---|---|
| roomId | string | 房间ID |

**Response 200:**
```json
{
  "roomId": "room_1718000000000_1",
  "name": "高手快来",
  "hostId": "1",
  "hostName": "Admin",
  "playerCount": 3,
  "maxPlayers": 6,
  "status": "waiting",
  "hasPassword": false,
  "gameDuration": 300,
  "gameMode": "multi",
  "players": [
    {
      "id": "1",
      "nickname": "Admin",
      "avatar": "https://example.com/avatar.png",
      "level": 5,
      "isHost": true,
      "isReady": true
    }
  ]
}
```

**Error 404:**
```json
{ "message": "房间不存在" }
```

---

## 05-匹配管理

### POST /api/matchmaking/join
加入匹配队列。需要 JWT token。

**Response 200（等待中）:**
```json
{
  "matched": false,
  "message": "已加入匹配队列",
  "queueSize": 3
}
```

**Response 200（匹配成功）:**
```json
{
  "matched": true,
  "roomId": "room_1718000000000_1",
  "message": "匹配成功"
}
```

**Error 401:**
```json
{ "message": "未登录" }
```

---

### POST /api/matchmaking/cancel
取消匹配。需要 JWT token。

**Response 200:**
```json
{ "message": "已取消匹配" }
```

**Error 401:**
```json
{ "message": "未登录" }
```

---

### GET /api/matchmaking/status
查询匹配状态（轮询用）。需要 JWT token。

**Response 200（匹配成功）:**
```json
{
  "matched": true,
  "roomId": "room_1718000000000_1",
  "message": "匹配成功"
}
```

**Response 200（等待中）:**
```json
{
  "matched": false,
  "inQueue": true,
  "queueSize": 3
}
```

**Error 401:**
```json
{ "message": "未登录" }
```

---

## 数据模型

### UserInfo（用户基本信息）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 用户ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| avatar | String | 头像URL |
| level | int | 等级 |
| totalScore | long | 总得分 |

### RoomSummary（房间摘要）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | String | 房间ID |
| name | String | 房间名称 |
| hostId | String | 房主用户ID |
| hostName | String | 房主昵称 |
| playerCount | int | 当前玩家数 |
| maxPlayers | int | 最大玩家数 |
| status | String | waiting(等待)/full(已满)/playing(游戏中)/finished(已结束) |
| hasPassword | boolean | 是否有密码 |
| gameDuration | int | 游戏时长（秒） |
| gameMode | String | multi(多人)/single(单人) |
| allowBots | boolean | 是否允许机器人 |

### PlayerInfo（玩家信息）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | String | 用户ID |
| nickname | String | 玩家昵称 |
| avatar | String | 头像URL |
| level | int | 等级 |
| isHost | boolean | 是否为房主 |
| isReady | boolean | 是否已准备 |

### 通用错误响应
| 状态码 | 说明 |
|---|---|
| 400 | 请求参数错误（如房间已满、密码错误等） |
| 401 | 未登录或 token 无效 |
| 403 | 权限不足（如账号被禁用） |
| 404 | 资源不存在 |

所有错误响应格式：
```json
{ "message": "错误描述" }
```

---

## 完整 API 端点总表

| 方法 | 路径 | 需要 Token | 说明 |
|---|---|---|---|
| POST | /api/auth/register | 否 | 用户注册 |
| POST | /api/auth/login | 否 | 用户登录 |
| GET | /api/auth/me | 是 | 获取当前用户信息 |
| POST | /api/auth/logout | 否 | 退出登录 |
| GET | /api/games | 否 | 获取游戏历史列表 |
| GET | /api/games/{gameId} | 否 | 获取游戏详情 |
| GET | /api/games/my-history | 是 | 获取我的游戏历史 |
| GET | /api/games/my-stats | 是 | 获取我的统计概览 |
| GET | /api/games/player/{userId}/history | 否 | 获取玩家游戏历史 |
| GET | /api/games/player/{userId}/stats | 否 | 获取玩家统计概览 |
| GET | /api/ranking | 否 | 获取排行榜 |
| GET | /api/ranking/my-rank | 是 | 获取我的排名 |
| GET | /api/ranking/user/{userId} | 否 | 获取用户排名 |
| GET | /api/rooms/list | 否 | 获取房间列表 |
| GET | /api/rooms/online-count | 否 | 获取在线人数 |
| POST | /api/rooms | 是 | 创建房间 |
| POST | /api/rooms/{roomId}/join | 是 | 加入房间 |
| POST | /api/rooms/{roomId}/leave | 是 | 退出房间 |
| PUT | /api/rooms/{roomId} | 是 | 更新房间设置 |
| GET | /api/rooms/{roomId} | 否 | 获取房间详情 |
| POST | /api/matchmaking/join | 是 | 加入匹配队列 |
| POST | /api/matchmaking/cancel | 是 | 取消匹配 |
| GET | /api/matchmaking/status | 是 | 查询匹配状态 |
