package com.yousheng.knowledgehub.tag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagUpdateRequest(
        @Schema(description = "标签名称")
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 30, message = "标签名称不能超过 30 个字符")
        String name
) {
}
