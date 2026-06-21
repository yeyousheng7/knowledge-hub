package com.yousheng.knowledgehub.ai.client;

import com.yousheng.knowledgehub.ai.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiEmbeddingClientConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldNotCreateClientWhenAiDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=false",
                        "app.ai.embedding.dimensions=3"
                )
                .run(context -> assertThat(context).doesNotHaveBean(EmbeddingClient.class));
    }

    @Test
    void shouldCreateClientWhenAiEnabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.embedding.dimensions=3"
                )
                .run(context -> assertThat(context).hasSingleBean(EmbeddingClient.class));
    }

    @Configuration
    @EnableConfigurationProperties(AiProperties.class)
    @Import(SpringAiEmbeddingClient.class)
    static class TestConfiguration {

        @Bean
        EmbeddingModel embeddingModel() {
            return Mockito.mock(EmbeddingModel.class);
        }
    }
}
