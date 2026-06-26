package com.yousheng.knowledgehub.ai.agent.operation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Objects;

public class AiAgentPendingOperationStore {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public AiAgentPendingOperationStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate, "stringRedisTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public void save(AiAgentPendingOperation operation, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue()
                    .set(toRedisKey(operation.userId(), operation.operationId()),
                            objectMapper.writeValueAsString(operation),
                            ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AI agent pending operation", e);
        }
    }

    private String toRedisKey(Long userId, String operationId) {
        return "ai:operation:" + userId + ":" + operationId;
    }
}
