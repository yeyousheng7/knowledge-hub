package com.yousheng.knowledgehub.ai.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

public class SpringAiChatClientAdapter implements AiChatClient {

    private static final String DEFAULT_SYSTEM = "你是 KnowledgeHub 的知识库助手。请严格根据用户提供的上下文回答。";

    private final ChatClient chatClient;

    public SpringAiChatClientAdapter(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(DEFAULT_SYSTEM)
                .build();
    }

    @Override
    public String chat(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }

        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        if (content == null || content.isBlank()) {
            throw new RuntimeException("AI chat returned empty content");
        }

        return content;
    }
}
