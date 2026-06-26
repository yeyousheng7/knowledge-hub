package com.yousheng.knowledgehub.ai.agent.dto;

import java.util.List;

public record ActionEnvelope(
        String answer,
        List<AiAgentAction> actions
) {
    public ActionEnvelope {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public AiAgentChatResponse toResponse() {
        return new AiAgentChatResponse(answer, actions);
    }
}
