package com.yousheng.knowledgehub.ai.config;

import com.yousheng.knowledgehub.ai.index.AiIndexGenerationService;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchService;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "openai")
@ConditionalOnProperty(prefix = "app.ai.index", name = "vector-store", havingValue = "redis")
public class AiIndexSearchConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AiIndexSearchService aiIndexSearchService(
            VectorStore vectorStore,
            AiIndexGenerationService generationService,
            AiProperties aiProperties,
            AppUserMapper appUserMapper) {
        return new AiIndexSearchService(vectorStore, generationService, aiProperties, appUserMapper);
    }
}
