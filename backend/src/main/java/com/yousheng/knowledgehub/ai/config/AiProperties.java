package com.yousheng.knowledgehub.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.ai")
public class AiProperties {
    private boolean enabled;
    private Chat chat;
    private Embedding embedding;
    private Index index;

    @Getter
    @Setter
    public static final class Chat {
        private String provider;
        private String baseUrl;
        private String apiKey;
        private String model;
    }

    @Getter
    @Setter
    public static final class Embedding {
        private int dimensions;
    }

    @Getter
    @Setter
    public static final class Index {
        private int chunkSize;
        private int chunkOverlap;
        private int topK;
        private String vectorIndexName;
        private String vectorStore;
    }

}
