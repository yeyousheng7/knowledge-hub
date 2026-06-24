package com.yousheng.knowledgehub.ai.config;

import com.yousheng.knowledgehub.ai.index.AiIndexWriterService;
import com.yousheng.knowledgehub.ai.index.AiNoteIndexSourceService;
import com.yousheng.knowledgehub.ai.index.NoteChunkDocumentMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPooled;

import static org.assertj.core.api.Assertions.assertThat;

class AiIndexWriterConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestDependencies.class, AiIndexWriterConfiguration.class);

    @Test
    void shouldNotCreateWriterWhenAiDisabled() {
        contextRunner
                .withBean(VectorStore.class, () -> Mockito.mock(VectorStore.class))
                .withPropertyValues(
                        "app.ai.enabled=false",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiIndexWriterService.class));
    }

    @Test
    void shouldNotCreateWriterWhenVectorStoreIsNotRedis() {
        contextRunner
                .withBean(VectorStore.class, () -> Mockito.mock(VectorStore.class))
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=none"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiIndexWriterService.class));
    }

    @Test
    void shouldNotCreateWriterWhenVectorStoreTypeIsMissing() {
        contextRunner
                .withBean(VectorStore.class, () -> Mockito.mock(VectorStore.class))
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiIndexWriterService.class));
    }

    @Test
    void shouldCreateWriterWhenConditionsMatchAndVectorStoreExists() {
        contextRunner
                .withBean(VectorStore.class, () -> Mockito.mock(VectorStore.class))
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis"
                )
                .run(context -> assertThat(context).hasSingleBean(AiIndexWriterService.class));
    }

    @Test
    void shouldFailFastWhenConditionsMatchButVectorStoreIsMissing() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("VectorStore");
                });
    }

    @Test
    void shouldCreateWriterWithManualRedisVectorStoreConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        TestDependencies.class,
                        AiRedisVectorStoreConfiguration.class,
                        AiIndexWriterConfiguration.class)
                .withBean(EmbeddingModel.class, () -> Mockito.mock(EmbeddingModel.class))
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis",
                        "spring.data.redis.host=localhost",
                        "spring.data.redis.port=6379",
                        "spring.data.redis.database=0",
                        "spring.ai.vectorstore.redis.initialize-schema=false",
                        "spring.ai.vectorstore.redis.index-name=kh_note_chunks",
                        "spring.ai.vectorstore.redis.prefix=kh:ai:note:chunk:"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisVectorStore.class);
                    assertThat(context).hasSingleBean(JedisPooled.class);
                    assertThat(context).hasSingleBean(AiIndexWriterService.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        AiNoteIndexSourceService sourceService() {
            return Mockito.mock(AiNoteIndexSourceService.class);
        }

        @Bean
        NoteChunkDocumentMapper noteChunkDocumentMapper() {
            return Mockito.mock(NoteChunkDocumentMapper.class);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return Mockito.mock(StringRedisTemplate.class);
        }
    }
}
