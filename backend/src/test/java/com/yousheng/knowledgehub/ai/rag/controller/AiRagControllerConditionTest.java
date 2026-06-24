package com.yousheng.knowledgehub.ai.rag.controller;

import com.yousheng.knowledgehub.ai.rag.AiRagService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiRagControllerConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiRagController.class)
            .withBean(AiRagService.class, () -> mock(AiRagService.class));

    @Test
    void shouldNotCreateControllerWhenAiDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=false",
                        "spring.ai.model.embedding=openai",
                        "spring.ai.model.chat=openai",
                        "app.ai.index.vector-store=redis",
                        "app.ai.rag.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiRagController.class));
    }

    @Test
    void shouldNotCreateControllerWhenRagDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "spring.ai.model.chat=openai",
                        "app.ai.index.vector-store=redis",
                        "app.ai.rag.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiRagController.class));
    }

    @Test
    void shouldNotCreateControllerWhenChatModelIsNone() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "spring.ai.model.chat=none",
                        "app.ai.index.vector-store=redis",
                        "app.ai.rag.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiRagController.class));
    }

    @Test
    void shouldCreateControllerWhenConditionsMatch() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "spring.ai.model.chat=openai",
                        "app.ai.index.vector-store=redis",
                        "app.ai.rag.enabled=true"
                )
                .run(context -> assertThat(context).hasSingleBean(AiRagController.class));
    }
}
