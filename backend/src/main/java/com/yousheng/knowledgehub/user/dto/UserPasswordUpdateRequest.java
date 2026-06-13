package com.yousheng.knowledgehub.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequest(
        @Schema(
                description = "旧密码，8 到 72 个字符",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "旧密码不能为空")
        @Size(min = 8, max = 72, message = "旧密码长度必须在 8 到 72 个字符之间")
        String oldPassword,

        @Schema(
                description = "新密码，8 到 72 个字符",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 72, message = "新密码长度必须在 8 到 72 个字符之间")
        String newPassword
) {
}
