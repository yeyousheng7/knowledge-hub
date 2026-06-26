package com.yousheng.knowledgehub.ai.agent;

import com.yousheng.knowledgehub.ai.agent.dto.AiAgentChatResponse;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AiAgentChatService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentChatService.class);

    private static final String AGENT_SYSTEM = """
            你是 KnowledgeHub 的笔记助手。
            只能使用工具读取当前用户自己的笔记，以及执行单篇笔记的发布与下架。
            不要编造不存在的笔记内容。
            如果工具返回 success=false，根据 code/message 向用户解释。
            如果列表结果不足，引导用户提供更具体关键词或翻页。
            单篇发布/下架可以调用对应工具；批量下架公开笔记必须调用 prepare_batch_unpublish_published_notes 生成待确认操作。
            不要声称已经执行批量下架，不要声称支持确认接口。
            不要声称已经创建、修改、删除笔记，不要声称支持未列出的批量操作。""";

    private final ChatClient chatClient;
    private final AiAgentSessionService sessionService;
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final AiAgentActionEnvelopeParser actionEnvelopeParser = new AiAgentActionEnvelopeParser();

    public AiAgentChatService(ChatModel chatModel, AiAgentSessionService sessionService,
                              MessageChatMemoryAdvisor memoryAdvisor, Object... tools) {
        this(chatModel, sessionService, memoryAdvisor, null, tools);
    }

    public AiAgentChatService(ChatModel chatModel, AiAgentSessionService sessionService,
                              MessageChatMemoryAdvisor memoryAdvisor,
                              ToolCallAdvisor toolCallAdvisor,
                              Object... tools) {
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService must not be null");
        this.memoryAdvisor = memoryAdvisor;
        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultSystem(AGENT_SYSTEM);
        if (tools.length > 0) {
            builder.defaultTools(tools);
        }
        List<Advisor> advisors = new ArrayList<>();
        if (memoryAdvisor != null) {
            advisors.add(memoryAdvisor);
        }
        if (toolCallAdvisor != null) {
            advisors.add(toolCallAdvisor);
        }
        if (!advisors.isEmpty()) {
            builder.defaultAdvisors(advisors);
        }
        this.chatClient = builder.build();
    }

    public AiAgentChatResponse chat(String message) {
        Long userId = sessionService.requireCurrentEnabledUserId();

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        try {
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                    .user(message);

            if (memoryAdvisor != null) {
                String conversationId = sessionService.generateConversationId(userId);
                spec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
            }

            String content = spec.call().content();

            if (content == null || content.isBlank()) {
                throw new BizException(ErrorCode.AI_CHAT_SERVICE_UNAVAILABLE, "AI 助手返回了空回复");
            }

            return actionEnvelopeParser.parse(content);
        } catch (BizException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("AI agent chat failed", e);
            throw new BizException(ErrorCode.AI_CHAT_SERVICE_UNAVAILABLE, e);
        }
    }
}
