package com.yousheng.knowledgehub.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @Schema(description = "分类名称，最大 30 字符")
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 30, message = "分类名称不能超过 30 个字符")
        String name
) {
}
