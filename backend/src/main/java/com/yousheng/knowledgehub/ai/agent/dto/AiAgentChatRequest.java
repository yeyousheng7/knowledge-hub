package com.yousheng.knowledgehub.ai.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAgentChatRequest(
        @NotBlank
        @Size(max = 1000)
        String message
) {
}
