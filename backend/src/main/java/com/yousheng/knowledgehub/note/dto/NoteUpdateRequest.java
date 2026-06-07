package com.yousheng.knowledgehub.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NoteUpdateRequest(
        @Schema(description = "笔记标题，最大 100 字符")
        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题不能超过 100 个字符")
        String title,

        @Schema(description = "Markdown 正文，最大 100000 字符")
        @Size(max = 100_000, message = "内容不能超过 100000 个字符")
        String contentMd,

        @Schema(description = "笔记摘要，最大 300 字符")
        @Size(max = 300, message = "摘要不能超过 300 个字符")
        String summary,

        @Schema(description = "分类 ID，null 表示未分类")
        Long categoryId,

        @Schema(description = "笔记标签列表")
        @Size(max = 10, message = "标签数量不能超过 10 个")
        List<Long> tagIds
) {
}
