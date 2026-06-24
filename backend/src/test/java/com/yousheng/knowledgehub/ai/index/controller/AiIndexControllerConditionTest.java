package com.yousheng.knowledgehub.ai.index.controller;

import com.yousheng.knowledgehub.ai.index.AiIndexWriterService;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiIndexControllerConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiIndexController.class)
            .withBean(AppUserMapper.class, () -> mock(AppUserMapper.class))
            .withBean(AiIndexWriterService.class, () -> mock(AiIndexWriterService.class));

    @Test
    void shouldNotCreateControllerWhenAiDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=false",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiIndexController.class));
    }

    @Test
    void shouldNotCreateControllerWhenEmbeddingModelDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=none",
                        "app.ai.index.vector-store=redis"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiIndexController.class));
    }

    @Test
    void shouldNotCreateControllerWhenVectorStoreIsNotRedis() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=none"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiIndexController.class));
    }

    @Test
    void shouldCreateControllerWhenConditionsMatchAndWriterExists() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis"
                )
                .run(context -> assertThat(context).hasSingleBean(AiIndexController.class));
    }
}
