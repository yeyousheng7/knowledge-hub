package com.yousheng.knowledgehub.ai.client;

import com.yousheng.knowledgehub.ai.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiEmbeddingClientTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Test
    void embed_validText_returnsEmbeddingVector() {
        SpringAiEmbeddingClient client = new SpringAiEmbeddingClient(embeddingModel, aiProperties(3));
        float[] vector = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingModel.embed("hello")).thenReturn(vector);

        float[] result = client.embed("hello");

        assertThat(result).containsExactly(0.1f, 0.2f, 0.3f);
        verify(embeddingModel).embed("hello");
    }

    @Test
    void embed_dimensionMismatch_throwsException() {
        SpringAiEmbeddingClient client = new SpringAiEmbeddingClient(embeddingModel, aiProperties(3));
        when(embeddingModel.embed("hello")).thenReturn(new float[]{0.1f, 0.2f});

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected 3 but was 2");
    }

    @Test
    void embed_nullVector_throwsException() {
        SpringAiEmbeddingClient client = new SpringAiEmbeddingClient(embeddingModel, aiProperties(3));
        when(embeddingModel.embed("hello")).thenReturn(null);

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void embed_blankText_rejectsRequestBeforeCallingModel() {
        SpringAiEmbeddingClient client = new SpringAiEmbeddingClient(embeddingModel, aiProperties(3));

        assertThatThrownBy(() -> client.embed(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");

        verify(embeddingModel, never()).embed(" ");
    }

    @Test
    void embed_missingDimensions_throwsException() {
        SpringAiEmbeddingClient client = new SpringAiEmbeddingClient(embeddingModel, aiProperties(0));
        when(embeddingModel.embed("hello")).thenReturn(new float[]{0.1f});

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dimensions must be positive");
    }

    private static AiProperties aiProperties(int dimensions) {
        AiProperties properties = new AiProperties();
        AiProperties.Embedding embedding = new AiProperties.Embedding();
        embedding.setDimensions(dimensions);
        properties.setEmbedding(embedding);
        return properties;
    }
}
