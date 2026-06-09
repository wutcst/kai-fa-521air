package com.snake.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 加入房间请求
 */
@Schema(description = "加入房间请求（有密码时需要 password）")
public class JoinRoomRequest {
    @Schema(description = "房间密码（有密码的房间必填）", example = "8888")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
