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
    private Rag rag;
    private Agent agent;

    @Getter
    @Setter
    public static final class Chat {
        private String provider;
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

    @Getter
    @Setter
    public static final class Rag {
        private boolean enabled;
    }

    @Getter
    @Setter
    public static final class Agent {
        private boolean enabled;
        private Memory memory;

        @Getter
        @Setter
        public static final class Memory {
            private boolean enabled;
            private int maxMessages = 20;
        }
    }
}
