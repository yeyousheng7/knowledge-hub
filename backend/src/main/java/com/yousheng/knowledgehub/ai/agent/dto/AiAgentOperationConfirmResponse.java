package com.yousheng.knowledgehub.ai.agent.dto;

import java.util.List;

public record AiAgentOperationConfirmResponse(
        String operationId,
        String operationType,
        String status,
        int affectedCount,
        List<AiAgentOperationAffectedItem> affectedItems,
        String message
) {
    public AiAgentOperationConfirmResponse {
        affectedItems = affectedItems == null ? List.of() : List.copyOf(affectedItems);
    }
}
