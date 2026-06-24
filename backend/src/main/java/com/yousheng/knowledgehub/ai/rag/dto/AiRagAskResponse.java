package com.yousheng.knowledgehub.ai.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "RAG 问答响应")
public record AiRagAskResponse(

        @Schema(description = "回答文本")
        String answer,

        @Schema(description = "参考来源列表")
        List<AiRagSourceResponse> sources
) {
}
