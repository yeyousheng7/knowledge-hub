package com.yousheng.knowledgehub.ai.agent;

import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

public class AiAgentChatService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentChatService.class);

    private static final String AGENT_SYSTEM = """
            你是 KnowledgeHub 的只读笔记助手。
            只能使用工具读取当前用户自己的笔记。
            不要编造不存在的笔记内容。
            如果工具返回 success=false，根据 code/message 向用户解释。
            如果列表结果不足，引导用户提供更具体关键词或翻页。
            不要声称已经创建、修改、删除、发布、下架笔记。""";

    private final ChatClient chatClient;

    public AiAgentChatService(ChatModel chatModel, Object... tools) {
        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultSystem(AGENT_SYSTEM);
        if (tools.length > 0) {
            builder.defaultTools(tools);
        }
        this.chatClient = builder.build();
    }

    public String chat(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        try {
            String content = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                throw new BizException(ErrorCode.AI_CHAT_SERVICE_UNAVAILABLE, "AI 助手返回了空回复");
            }

            return content;
        } catch (BizException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("AI agent chat failed", e);
            throw new BizException(ErrorCode.AI_CHAT_SERVICE_UNAVAILABLE, e);
        }
    }
}
