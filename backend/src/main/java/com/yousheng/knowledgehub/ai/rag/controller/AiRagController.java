package com.yousheng.knowledgehub.ai.rag.controller;

import com.yousheng.knowledgehub.ai.rag.AiRagAnswer;
import com.yousheng.knowledgehub.ai.rag.AiRagService;
import com.yousheng.knowledgehub.ai.rag.dto.AiRagAskRequest;
import com.yousheng.knowledgehub.ai.rag.dto.AiRagAskResponse;
import com.yousheng.knowledgehub.ai.rag.dto.AiRagSourceResponse;
import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.config.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ai-rag", description = "AI 笔记问答接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/ai/rag")
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "openai")
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
@ConditionalOnProperty(prefix = "app.ai.index", name = "vector-store", havingValue = "redis")
@ConditionalOnProperty(prefix = "app.ai.rag", name = "enabled", havingValue = "true")
public class AiRagController {

    private final AiRagService aiRagService;

    @PostMapping("/ask")
    public ApiResponse<AiRagAskResponse> ask(@Valid @RequestBody AiRagAskRequest request) {
        AiRagAnswer answer = aiRagService.ask(request.question());
        return ApiResponse.ok(toResponse(answer));
    }

    private AiRagAskResponse toResponse(AiRagAnswer answer) {
        return new AiRagAskResponse(
                answer.answer(),
                answer.sources().stream()
                        .map(s -> new AiRagSourceResponse(
                                s.noteId(),
                                s.title(),
                                s.snippet(),
                                s.chunkIndex(),
                                s.distance(),
                                s.visibility(),
                                s.updatedAt()
                        ))
                        .toList()
        );
    }
}
