package com.yousheng.knowledgehub.ai.agent.controller;

import com.yousheng.knowledgehub.ai.agent.AiAgentChatService;
import com.yousheng.knowledgehub.ai.agent.AiAgentSessionService;
import com.yousheng.knowledgehub.ai.agent.dto.AiAgentChatRequest;
import com.yousheng.knowledgehub.ai.agent.dto.AiAgentChatResponse;
import com.yousheng.knowledgehub.ai.agent.dto.SessionClearResponse;
import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.config.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ai-agent", description = "AI Agent 对话接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/ai/agent")
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "app.ai.agent", name = "enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
@ConditionalOnExpression("'${app.ai.chat.provider:deepseek}'.equals('deepseek') || '${app.ai.chat.provider:deepseek}'.equals('openai-compatible')")
public class AiAgentController {

    private final AiAgentChatService aiAgentChatService;
    private final AiAgentSessionService aiAgentSessionService;

    @PostMapping("/chat")
    public ApiResponse<AiAgentChatResponse> chat(@Valid @RequestBody AiAgentChatRequest request) {
        String answer = aiAgentChatService.chat(request.message());
        return ApiResponse.ok(new AiAgentChatResponse(answer));
    }

    @PostMapping("/session/clear")
    public ApiResponse<SessionClearResponse> clearSession() {
        aiAgentSessionService.clearSession();
        return ApiResponse.ok(new SessionClearResponse(true));
    }
}
