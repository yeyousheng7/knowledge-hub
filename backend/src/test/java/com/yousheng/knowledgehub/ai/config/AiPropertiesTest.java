package com.yousheng.knowledgehub.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.ai.enabled=true",
        "app.ai.chat.provider=openai",
        "app.ai.chat.base-url=https://api.openai.com",
        "app.ai.chat.api-key=sk-test-chat-key",
        "app.ai.chat.model=gpt-4",
        "app.ai.embedding.provider=openai",
        "app.ai.embedding.base-url=https://api.openai.com",
        "app.ai.embedding.api-key=sk-test-embedding-key",
        "app.ai.embedding.model=text-embedding-3-small"
})
class AiPropertiesTest {

    @Autowired
    private AiProperties aiProperties;

    @Test
    void shouldBindEnabled() {
        assertThat(aiProperties.isEnabled()).isTrue();
    }

    @Test
    void shouldBindChatProperties() {
        assertThat(aiProperties.getChat()).isNotNull();
        assertThat(aiProperties.getChat().getProvider()).isEqualTo("openai");
        assertThat(aiProperties.getChat().getBaseUrl()).isEqualTo("https://api.openai.com");
        assertThat(aiProperties.getChat().getApiKey()).isEqualTo("sk-test-chat-key");
        assertThat(aiProperties.getChat().getModel()).isEqualTo("gpt-4");
    }

    @Test
    void shouldBindEmbeddingProperties() {
        assertThat(aiProperties.getEmbedding()).isNotNull();
        assertThat(aiProperties.getEmbedding().getProvider()).isEqualTo("openai");
        assertThat(aiProperties.getEmbedding().getBaseUrl()).isEqualTo("https://api.openai.com");
        assertThat(aiProperties.getEmbedding().getApiKey()).isEqualTo("sk-test-embedding-key");
        assertThat(aiProperties.getEmbedding().getModel()).isEqualTo("text-embedding-3-small");
    }
}
