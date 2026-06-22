package com.yousheng.knowledgehub.ai.client;

import com.yousheng.knowledgehub.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@Service
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "spring.ai.model", name = "embedding", havingValue = "openai")
public class SpringAiEmbeddingClient implements EmbeddingClient {

    private final EmbeddingModel embeddingModel;
    private final AiProperties aiProperties;

    @Override
    public float[] embed(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("text must not be blank");
        }

        float[] vector = embeddingModel.embed(text);
        validateDimensions(vector);
        return vector;
    }

    private void validateDimensions(float[] vector) {
        if (vector == null) {
            throw new IllegalStateException("embedding vector must not be null");
        }

        int expectedDimensions = expectedDimensions();
        if (vector.length != expectedDimensions) {
            throw new IllegalStateException(
                    "embedding vector dimensions mismatch: expected %d but was %d"
                            .formatted(expectedDimensions, vector.length)
            );
        }
    }

    private int expectedDimensions() {
        AiProperties.Embedding embedding = aiProperties.getEmbedding();
        if (embedding == null || embedding.getDimensions() <= 0) {
            throw new IllegalStateException("app.ai.embedding.dimensions must be positive");
        }
        return embedding.getDimensions();
    }
}
