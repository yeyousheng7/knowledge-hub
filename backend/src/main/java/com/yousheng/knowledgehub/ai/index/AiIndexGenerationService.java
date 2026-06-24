package com.yousheng.knowledgehub.ai.index;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

@RequiredArgsConstructor
public class AiIndexGenerationService {

    private static final String ACTIVE_GENERATION_KEY_PREFIX = "ai:index:active-generation:";

    private final StringRedisTemplate stringRedisTemplate;

    public String getActiveGeneration(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        return stringRedisTemplate.opsForValue().get(activeGenerationKey(userId));
    }

    public void activateGeneration(Long userId, String generation) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (generation == null || generation.isBlank()) {
            throw new IllegalArgumentException("generation must not be blank");
        }

        stringRedisTemplate.opsForValue().set(activeGenerationKey(userId), generation);
    }

    private static String activeGenerationKey(Long userId) {
        return ACTIVE_GENERATION_KEY_PREFIX + userId;
    }
}
