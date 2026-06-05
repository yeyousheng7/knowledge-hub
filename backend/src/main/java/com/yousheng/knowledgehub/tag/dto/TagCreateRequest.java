package com.yousheng.knowledgehub.tag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagCreateRequest(
        @Schema(description = "Tag 名称")
        @NotBlank(message = "Tag 名称不能为空")
        @Size(max = 30, message = "Tag 名称至多为 30 个字符")
        String name
) {
}
