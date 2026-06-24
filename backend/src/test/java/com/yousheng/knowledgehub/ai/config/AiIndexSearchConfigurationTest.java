package com.yousheng.knowledgehub.ai.config;

import com.yousheng.knowledgehub.ai.index.AiIndexGenerationService;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchService;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AiIndexSearchConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestDependencies.class, AiIndexSearchConfiguration.class);

    @Test
    void shouldNotCreateSearchServiceWhenAiDisabled() {
        contextRunner
                .withBean(VectorStore.class, () -> Mockito.mock(VectorStore.class))
                .withPropertyValues(
                        "app.ai.enabled=false",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiIndexSearchService.class));
    }

    @Test
    void shouldNotCreateSearchServiceWhenVectorStoreIsNotRedis() {
        contextRunner
                .withBean(VectorStore.class, () -> Mockito.mock(VectorStore.class))
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=none"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiIndexSearchService.class));
    }

    @Test
    void shouldCreateSearchServiceWhenConditionsMatch() {
        contextRunner
                .withBean(VectorStore.class, () -> Mockito.mock(VectorStore.class))
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "spring.ai.model.embedding=openai",
                        "app.ai.index.vector-store=redis"
                )
                .run(context -> assertThat(context).hasSingleBean(AiIndexSearchService.class));
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

    @Configuration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return Mockito.mock(StringRedisTemplate.class);
        }

        @Bean
        AiIndexGenerationService aiIndexGenerationService(StringRedisTemplate stringRedisTemplate) {
            return new AiIndexGenerationService(stringRedisTemplate);
        }

        @Bean
        AiProperties aiProperties() {
            return Mockito.mock(AiProperties.class);
        }

        @Bean
        AppUserMapper appUserMapper() {
            return Mockito.mock(AppUserMapper.class);
        }
    }
}
