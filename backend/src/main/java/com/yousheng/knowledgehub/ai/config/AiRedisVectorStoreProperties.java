package com.yousheng.knowledgehub.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("spring.ai.vectorstore.redis")
public class AiRedisVectorStoreProperties {

    private boolean initializeSchema;
    private String indexName = RedisVectorStore.DEFAULT_INDEX_NAME;
    private String prefix = RedisVectorStore.DEFAULT_PREFIX;
}
