package com.yousheng.knowledgehub.ai.config;

import com.yousheng.knowledgehub.ai.index.AiIndexGenerationService;
import com.yousheng.knowledgehub.ai.index.AiIndexWriterService;
import com.yousheng.knowledgehub.ai.index.AiNoteIndexSourceService;
import com.yousheng.knowledgehub.ai.index.NoteChunkDocumentMapper;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "openai")
@ConditionalOnProperty(prefix = "app.ai.index", name = "vector-store", havingValue = "redis")
public class AiIndexWriterConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AiIndexGenerationService aiIndexGenerationService(StringRedisTemplate stringRedisTemplate) {
        return new AiIndexGenerationService(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiIndexWriterService aiIndexWriterService(
            VectorStore vectorStore,
            AiNoteIndexSourceService sourceService,
            NoteChunkDocumentMapper documentMapper,
            AiIndexGenerationService generationService) {
        return new AiIndexWriterService(vectorStore, sourceService, documentMapper, generationService);
    }
}
