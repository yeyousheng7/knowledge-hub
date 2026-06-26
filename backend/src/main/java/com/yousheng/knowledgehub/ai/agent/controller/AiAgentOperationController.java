package com.yousheng.knowledgehub.ai.agent.controller;

import com.yousheng.knowledgehub.ai.agent.AiAgentOperationConfirmService;
import com.yousheng.knowledgehub.ai.agent.dto.AiAgentOperationConfirmResponse;
import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.config.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ai-agent-operation", description = "AI Agent 操作确认接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/ai/operations")
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "app.ai.agent", name = "enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
@ConditionalOnExpression("'${app.ai.chat.provider:deepseek}'.equals('deepseek') || '${app.ai.chat.provider:deepseek}'.equals('openai-compatible')")
public class AiAgentOperationController {

    private final AiAgentOperationConfirmService confirmService;

    @PostMapping("/{operationId}/confirm")
    public ApiResponse<AiAgentOperationConfirmResponse> confirm(@PathVariable String operationId) {
        return ApiResponse.ok(confirmService.confirm(operationId));
    }
}
