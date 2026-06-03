package com.yousheng.knowledgehub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "访问令牌")
        String accessToken,
        @Schema(description = "令牌类型，固定为 Bearer")
        String tokenType,
        @Schema(description = "访问令牌有效期，单位为秒")
        long expiresIn,
        @Schema(description = "登录用户信息")
        LoginUserResponse user
) {
}
