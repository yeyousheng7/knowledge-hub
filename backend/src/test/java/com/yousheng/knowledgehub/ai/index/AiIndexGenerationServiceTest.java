package com.yousheng.knowledgehub.ai.index;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiIndexGenerationServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void getActiveGeneration_readsUserGenerationKey() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai:index:active-generation:10")).thenReturn("gen-old");

        String generation = service().getActiveGeneration(10L);

        assertThat(generation).isEqualTo("gen-old");
    }

    @Test
    void activateGeneration_writesUserGenerationKey() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        service().activateGeneration(10L, "gen-new");

        verify(valueOperations).set("ai:index:active-generation:10", "gen-new");
    }

    @Test
    void getActiveGeneration_nullUserId_throwsException() {
        assertThatThrownBy(() -> service().getActiveGeneration(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId must not be null");
    }

    @Test
    void activateGeneration_blankGeneration_throwsException() {
        assertThatThrownBy(() -> service().activateGeneration(10L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("generation must not be blank");
    }

    private AiIndexGenerationService service() {
        return new AiIndexGenerationService(stringRedisTemplate);
    }
}
