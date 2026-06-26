package com.yousheng.knowledgehub.ai.agent.dto;

import java.util.Map;

public record AgentAction(
        String type,
        Map<String, Object> payload
) {
    public AgentAction {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
