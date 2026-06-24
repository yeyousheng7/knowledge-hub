package com.yousheng.knowledgehub.ai.config;

import com.yousheng.knowledgehub.ai.chat.AiChatClient;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchService;
import com.yousheng.knowledgehub.ai.rag.AiRagService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AiRagConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestDependencies.class, AiRagConfiguration.class);

    @Test
    void shouldNotCreateRagServiceWhenRagDisabled() {
        contextRunner
                .withBean(AiChatClient.class, () -> Mockito.mock(AiChatClient.class))
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis",
                        "app.ai.rag.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiRagService.class));
    }

    @Test
    void shouldCreateRagServiceWhenRagEnabledAndConditionsMatch() {
        contextRunner
                .withBean(AiChatClient.class, () -> Mockito.mock(AiChatClient.class))
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis",
                        "app.ai.rag.enabled=true"
                )
                .run(context -> assertThat(context).hasSingleBean(AiRagService.class));
    }

    @Test
    void shouldNotCreateRagServiceWhenAiDisabled() {
        contextRunner
                .withBean(AiChatClient.class, () -> Mockito.mock(AiChatClient.class))
                .withPropertyValues(
                        "app.ai.enabled=false",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis",
                        "app.ai.rag.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiRagService.class));
    }

    @Test
    void shouldNotCreateRagServiceWhenVectorStoreIsNotRedis() {
        contextRunner
                .withBean(AiChatClient.class, () -> Mockito.mock(AiChatClient.class))
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=none",
                        "app.ai.rag.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiRagService.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        AiIndexSearchService aiIndexSearchService() {
            return Mockito.mock(AiIndexSearchService.class);
        }
    }
}
