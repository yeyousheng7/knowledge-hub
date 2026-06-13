package com.yousheng.knowledgehub.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(min = 3, max = 30, message = "昵称长度必须在 3 到 30 个字符之间")
        @Pattern(regexp = ".*\\S.*", message = "昵称不能全为空白")
        String nickname,

        @Size(max = 60, message = "个人简介不能超过 60 个字符")
        String bio
) {
}
