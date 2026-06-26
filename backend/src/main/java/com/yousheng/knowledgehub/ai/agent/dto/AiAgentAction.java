package com.yousheng.knowledgehub.ai.agent.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record AiAgentAction(
        String type,
        Map<String, Object> payload
) {
    public AiAgentAction {
        payload = payload == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
}
