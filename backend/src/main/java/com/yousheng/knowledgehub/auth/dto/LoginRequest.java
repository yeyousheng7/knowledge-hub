package com.yousheng.knowledgehub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(description = "用户名")
        @NotBlank
        @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "用户名只能包含字母、数字和下划线")
        @Size(min = 3, max = 30, message = "用户名长度必须在 3 到 30 个字符之间")
        String username,

        @Schema(
                description = "密码",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank
        @Size(min = 8, max = 72, message = "密码长度必须在 8 到 72 个字符之间")
        String password
) {
}
