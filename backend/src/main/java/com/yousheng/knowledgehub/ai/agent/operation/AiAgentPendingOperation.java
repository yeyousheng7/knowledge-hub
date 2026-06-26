package com.yousheng.knowledgehub.ai.agent.operation;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AiAgentPendingOperation(
        String operationId,
        String operationType,
        Long userId,
        List<Long> noteIds,
        Map<String, Object> payload,
        Instant createdAt,
        Instant expiresAt,
        String status
) {
    public AiAgentPendingOperation(String operationId,
                                   String operationType,
                                   Long userId,
                                   List<Long> noteIds,
                                   Instant createdAt,
                                   Instant expiresAt,
                                   String status) {
        this(operationId, operationType, userId, noteIds, Map.of(), createdAt, expiresAt, status);
    }

    public AiAgentPendingOperation {
        noteIds = noteIds == null ? List.of() : List.copyOf(noteIds);
        payload = payload == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
}
