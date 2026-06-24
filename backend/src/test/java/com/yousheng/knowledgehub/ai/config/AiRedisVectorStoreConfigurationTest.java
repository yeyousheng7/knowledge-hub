package com.yousheng.knowledgehub.ai.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore.MetadataField;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.ClassUtils;
import org.springframework.test.util.ReflectionTestUtils;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Schema.FieldType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiRedisVectorStoreConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiRedisVectorStoreConfiguration.class)
            .withBean(EmbeddingModel.class, () -> Mockito.mock(EmbeddingModel.class))
            .withPropertyValues(
                    "spring.data.redis.host=localhost",
                    "spring.data.redis.port=6379",
                    "spring.data.redis.database=0",
                    "spring.ai.vectorstore.redis.initialize-schema=false",
                    "spring.ai.vectorstore.redis.index-name=kh_note_chunks",
                    "spring.ai.vectorstore.redis.prefix=kh:ai:note:chunk:"
            );

    @Test
    void shouldNotCreateVectorStoreWhenAiDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=false",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RedisVectorStore.class);
                    assertThat(context).doesNotHaveBean(JedisPooled.class);
                });
    }

    @Test
    void shouldNotCreateVectorStoreWhenEmbeddingModelDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=none",
                        "app.ai.index.vector-store=redis"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RedisVectorStore.class);
                    assertThat(context).doesNotHaveBean(JedisPooled.class);
                });
    }

    @Test
    void shouldNotCreateVectorStoreWhenVectorStoreTypeIsNotRedis() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=none"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RedisVectorStore.class);
                    assertThat(context).doesNotHaveBean(JedisPooled.class);
                });
    }

    @Test
    void shouldNotCreateVectorStoreWhenVectorStoreTypeIsMissing() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RedisVectorStore.class);
                    assertThat(context).doesNotHaveBean(JedisPooled.class);
                });
    }

    @Test
    void shouldNotHaveSpringAiRedisVectorStoreAutoConfigurationOnClasspath() {
        contextRunner.run(context -> assertThat(ClassUtils.isPresent(
                "org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreAutoConfiguration",
                context.getClassLoader())).isFalse());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateRedisVectorStoreWithMetadataFieldsWhenAllConditionsMatch() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisVectorStore.class);
                    RedisVectorStore vectorStore = context.getBean(RedisVectorStore.class);

                    assertThat(ReflectionTestUtils.getField(vectorStore, "indexName"))
                            .isEqualTo("kh_note_chunks");
                    assertThat(ReflectionTestUtils.getField(vectorStore, "prefix"))
                            .isEqualTo("kh:ai:note:chunk:");
                    assertThat(ReflectionTestUtils.getField(vectorStore, "initializeSchema"))
                            .isEqualTo(false);

                    List<MetadataField> metadataFields = (List<MetadataField>) ReflectionTestUtils
                            .getField(vectorStore, "metadataFields");

                    assertThat(metadataFields)
                            .extracting(MetadataField::name)
                            .containsExactly(
                                    "userId",
                                    "noteId",
                                    "title",
                                    "chunkIndex",
                                    "visibility",
                                    "updatedAt",
                                    "contentHash",
                                    "indexGeneration"
                            );
                    assertThat(metadataFields)
                            .extracting(MetadataField::fieldType)
                            .containsExactly(
                                    FieldType.NUMERIC,
                                    FieldType.NUMERIC,
                                    FieldType.TEXT,
                                    FieldType.NUMERIC,
                                    FieldType.TAG,
                                    FieldType.TEXT,
                                    FieldType.TAG,
                                    FieldType.TAG
                            );
                });
    }
}
