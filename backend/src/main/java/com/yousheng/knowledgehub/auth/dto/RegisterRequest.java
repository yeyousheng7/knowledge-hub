package com.yousheng.knowledgehub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(description = "用户名，3 到 30 个字符，仅允许字母、数字和下划线")
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 30, message = "用户名长度必须在 3 到 30 个字符之间")
        @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @Schema(
                description = "密码，8 到 72 个字符",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度必须在 8 到 72 个字符之间")
        String password,

        @Schema(description = "昵称，3 到 30 个字符，可为空；为空时默认使用用户名")
        @Size(min = 3, max = 30, message = "昵称长度必须在 3 到 30 个字符之间")
        @Pattern(regexp = ".*\\S.*", message = "昵称不能全为空白")
        String nickname,

        @Schema(description = "注册邀请码")
        @NotBlank(message = "邀请码不能为空")
        String inviteCode
) {
}
