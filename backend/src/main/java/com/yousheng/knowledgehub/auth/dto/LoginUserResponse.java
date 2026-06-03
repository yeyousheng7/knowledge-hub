package com.yousheng.knowledgehub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginUserResponse(
        @Schema(description = "用户 ID")
        Long id,
        @Schema(description = "用户名")
        String username,
        @Schema(description = "昵称")
        String nickname,
        @Schema(description = "用户角色，USER 或 ADMIN")
        String role
) {
}
