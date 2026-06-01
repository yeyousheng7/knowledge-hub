package com.yousheng.knowledgehub.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteCreateRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题不能超过 100 个字符")
        String title,

        @Size(max = 100_000, message = "正文不能超过 100000 个字符")
        String contentMd,

        @Size(max = 300, message = "摘要不能超过 300 个字符")
        String summary
) {
}
