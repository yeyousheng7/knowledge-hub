package com.yousheng.knowledgehub.ai.config;

import com.yousheng.knowledgehub.ai.chat.AiChatClient;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchService;
import com.yousheng.knowledgehub.ai.rag.AiRagService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "openai")
@ConditionalOnProperty(prefix = "app.ai.index", name = "vector-store", havingValue = "redis")
@ConditionalOnProperty(prefix = "app.ai.rag", name = "enabled", havingValue = "true")
public class AiRagConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AiRagService aiRagService(
            AiIndexSearchService searchService,
            AiChatClient chatClient) {
        return new AiRagService(searchService, chatClient);
    }
}
