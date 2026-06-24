package com.yousheng.knowledgehub.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore.MetadataField;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({RedisVectorStore.class, JedisPooled.class, EmbeddingModel.class})
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "openai")
@ConditionalOnProperty(prefix = "app.ai.index", name = "vector-store", havingValue = "redis")
@EnableConfigurationProperties({RedisProperties.class, AiRedisVectorStoreProperties.class})
public class AiRedisVectorStoreConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(name = "aiRedisVectorStoreJedisPooled")
    public JedisPooled aiRedisVectorStoreJedisPooled(RedisProperties redisProperties) {
        DefaultJedisClientConfig.Builder clientConfig = DefaultJedisClientConfig.builder()
                .database(redisProperties.getDatabase())
                .ssl(redisProperties.getSsl().isEnabled());

        if (redisProperties.getTimeout() != null) {
            clientConfig.timeoutMillis(toMillis(redisProperties.getTimeout()));
        }
        if (redisProperties.getConnectTimeout() != null) {
            clientConfig.connectionTimeoutMillis(toMillis(redisProperties.getConnectTimeout()));
        }
        if (StringUtils.hasText(redisProperties.getUsername())) {
            clientConfig.user(redisProperties.getUsername());
        }
        if (StringUtils.hasText(redisProperties.getPassword())) {
            clientConfig.password(redisProperties.getPassword());
        }
        if (StringUtils.hasText(redisProperties.getClientName())) {
            clientConfig.clientName(redisProperties.getClientName());
        }

        return new JedisPooled(new HostAndPort(redisProperties.getHost(), redisProperties.getPort()),
                clientConfig.build());
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public RedisVectorStore redisVectorStore(
            @Qualifier("aiRedisVectorStoreJedisPooled") JedisPooled jedisPooled,
            EmbeddingModel embeddingModel,
            AiRedisVectorStoreProperties redisVectorStoreProperties) {

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .initializeSchema(redisVectorStoreProperties.isInitializeSchema())
                .indexName(redisVectorStoreProperties.getIndexName())
                .prefix(redisVectorStoreProperties.getPrefix())
                .metadataFields(
                        MetadataField.numeric("userId"),
                        MetadataField.numeric("noteId"),
                        MetadataField.text("title"),
                        MetadataField.numeric("chunkIndex"),
                        MetadataField.tag("visibility"),
                        MetadataField.text("updatedAt"),
                        MetadataField.tag("contentHash"),
                        MetadataField.tag("indexGeneration"))
                .build();
    }

    private static int toMillis(Duration duration) {
        return Math.toIntExact(duration.toMillis());
    }
}
