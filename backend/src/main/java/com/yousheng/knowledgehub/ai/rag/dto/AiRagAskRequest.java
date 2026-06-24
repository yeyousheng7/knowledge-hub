package com.yousheng.knowledgehub.ai.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "RAG 问答请求")
public record AiRagAskRequest(

        @Schema(description = "用户问题", example = "什么是 Spring AI？")
        @NotBlank(message = "问题不能为空")
        @Size(max = 1000, message = "问题长度不能超过1000")
        String question
) {
}
