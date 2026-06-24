package com.yousheng.knowledgehub.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class AiPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "app.ai.enabled=true",
                    "app.ai.chat.provider=openai",
                    "app.ai.embedding.dimensions=1536",
                    "app.ai.index.chunk-size=1024",
                    "app.ai.index.chunk-overlap=50",
                    "app.ai.index.top-k=3",
                    "app.ai.index.vector-index-name=kh_note_chunks",
                    "app.ai.index.vector-store=redis"
            );

    @Test
    void shouldBindEnabled() {
        withAiProperties(aiProperties -> assertThat(aiProperties.isEnabled()).isTrue());
    }

    @Test
    void shouldBindChatProperties() {
        withAiProperties(aiProperties -> {
            assertThat(aiProperties.getChat()).isNotNull();
            assertThat(aiProperties.getChat().getProvider()).isEqualTo("openai");
        });
    }

    @Test
    void shouldBindEmbeddingProperties() {
        withAiProperties(aiProperties -> {
            assertThat(aiProperties.getEmbedding()).isNotNull();
            assertThat(aiProperties.getEmbedding().getDimensions()).isEqualTo(1536);
        });
    }

    @Test
    void shouldBindIndexProperties() {
        withAiProperties(aiProperties -> {
            assertThat(aiProperties.getIndex()).isNotNull();
            assertThat(aiProperties.getIndex().getChunkSize()).isEqualTo(1024);
            assertThat(aiProperties.getIndex().getChunkOverlap()).isEqualTo(50);
            assertThat(aiProperties.getIndex().getTopK()).isEqualTo(3);
            assertThat(aiProperties.getIndex().getVectorIndexName()).isEqualTo("kh_note_chunks");
            assertThat(aiProperties.getIndex().getVectorStore()).isEqualTo("redis");
        });
    }

    private void withAiProperties(Consumer<AiProperties> assertion) {
        contextRunner.run(context -> assertion.accept(context.getBean(AiProperties.class)));
    }

    @Configuration
    @EnableConfigurationProperties(AiProperties.class)
    static class TestConfiguration {
    }
}
