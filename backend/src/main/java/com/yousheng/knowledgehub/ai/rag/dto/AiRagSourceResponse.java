package com.yousheng.knowledgehub.ai.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "RAG 问答参考来源")
public record AiRagSourceResponse(

        @Schema(description = "笔记 ID")
        Long noteId,

        @Schema(description = "笔记标题")
        String title,

        @Schema(description = "匹配片段")
        String snippet,

        @Schema(description = "片段索引")
        int chunkIndex,

        @Schema(description = "向量距离")
        Double distance,

        @Schema(description = "可见性")
        String visibility,

        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
