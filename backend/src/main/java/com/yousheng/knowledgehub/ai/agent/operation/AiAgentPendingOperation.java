package com.yousheng.knowledgehub.ai.agent.operation;

import java.time.Instant;
import java.util.List;

public record AiAgentPendingOperation(
        String operationId,
        String operationType,
        Long userId,
        List<Long> noteIds,
        Instant createdAt,
        Instant expiresAt,
        String status
) {
    public AiAgentPendingOperation {
        noteIds = noteIds == null ? List.of() : List.copyOf(noteIds);
    }
}
