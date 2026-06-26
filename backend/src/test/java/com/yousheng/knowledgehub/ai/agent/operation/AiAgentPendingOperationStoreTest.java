package com.yousheng.knowledgehub.ai.agent.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiAgentPendingOperationStoreTest {

    private final Map<String, String> redis = new HashMap<>();

    private AiAgentPendingOperationStore store;

    @BeforeEach
    void setUp() {
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doAnswer(invocation -> {
            redis.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(valueOperations.getAndDelete(anyString()))
                .thenAnswer(invocation -> redis.remove(invocation.getArgument(0)));

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        store = new AiAgentPendingOperationStore(stringRedisTemplate, objectMapper);
    }

    @Test
    void consume_isOneTime() {
        AiAgentPendingOperation operation = new AiAgentPendingOperation(
                "op-1",
                "BATCH_UNPUBLISH_NOTES",
                7L,
                List.of(1L, 2L),
                Instant.parse("2026-06-26T12:00:00Z"),
                Instant.parse("2026-06-26T12:30:00Z"),
                "PENDING");
        store.save(operation, Duration.ofMinutes(30));

        Optional<AiAgentPendingOperation> first = store.consume(7L, "op-1");
        Optional<AiAgentPendingOperation> second = store.consume(7L, "op-1");

        assertThat(first).isPresent();
        assertThat(first.get().noteIds()).containsExactly(1L, 2L);
        assertThat(second).isEmpty();
    }
}
