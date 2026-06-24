package com.yousheng.knowledgehub.ai.config;

import com.yousheng.knowledgehub.ai.chat.AiChatClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatClientConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestChatModelConfig.class, AiChatClientConfiguration.class);

    @Test
    void shouldNotCreateAiChatClientWhenAiDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=false",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiChatClient.class));
    }

    @Test
    void shouldNotCreateAiChatClientWhenChatModelIsNone() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.chat=none",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiChatClient.class));
    }

    @Test
    void shouldNotCreateAiChatClientWhenProviderMismatch() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=unknown-provider"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiChatClient.class));
    }

    @Test
    void shouldCreateAiChatClientWhenConditionsMatchDeepSeek() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> assertThat(context).hasSingleBean(AiChatClient.class));
    }

    @Test
    void shouldCreateAiChatClientWhenConditionsMatchOpenAiCompatible() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=openai-compatible"
                )
                .run(context -> assertThat(context).hasSingleBean(AiChatClient.class));
    }

    @Test
    void shouldCreateAiChatClientWhenFollowingDocEnvSetup() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "spring.ai.model.chat=openai",
                        "app.ai.index.vector-store=redis",
                        "app.ai.rag.enabled=true",
                        "app.ai.chat.provider=deepseek",
                        "spring.ai.openai.chat.base-url=https://api.deepseek.com",
                        "spring.ai.openai.chat.api-key=sk-test-key",
                        "spring.ai.openai.chat.options.model=deepseek-chat"
                )
                .run(context -> assertThat(context).hasSingleBean(AiChatClient.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestChatModelConfig {

        @Bean
        ChatModel chatModel() {
            return Mockito.mock(ChatModel.class);
        }
    }
}
