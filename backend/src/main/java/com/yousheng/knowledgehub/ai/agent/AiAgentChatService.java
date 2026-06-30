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

    private static final String SYSTEM_BASE = """
            你是 KnowledgeHub 的笔记助手。
            只能使用工具读取当前用户自己的笔记、搜索系统公开笔记，以及执行已明确支持的笔记操作。
            不要编造不存在的笔记内容。
            如果工具返回 success=false，根据 code/message 向用户解释。
            如果列表结果不足，引导用户提供更具体关键词或翻页。
            """;
    
    private static final String SYSTEM_TOOL_STRATEGY = """
        你不是普通聊天机器人，而是能够主动使用工具的知识库 Agent。

        当用户的问题与知识、学习、技术主题、项目经验、笔记内容、公开内容、阅读建议、复习建议有关时，
        即使用户没有明确说“搜索”“查找”“查看笔记”，也应主动考虑使用工具获取上下文，而不是直接凭通用知识回答。

        对模糊主题请求，应先自行推断合理关键词并进行检索。
        例如用户说“我想了解 Spring”，可以先围绕 "Spring"、"Spring Boot" 或用户原始问题进行检索；
        用户说“我想复习 Redis”，可以先检索 "Redis"；
        用户说“我最近写过 JWT 吗”，应搜索当前用户自己的笔记。

        当用户问“我有哪些笔记”“浏览一下我的笔记”等无特定关键词的浏览需求时，使用 list_my_notes。
        当用户问“我发布了哪些笔记”时，使用 list_my_published_notes。

        工具使用优先级：
        1. 如果问题可能与当前用户自己的知识积累有关，优先使用 search_my_notes。
        2. 如果需要完整内容、引用依据或进一步分析，应根据搜索结果调用 get_my_note_detail。
        3. 如果用户自己的笔记不足，或用户明确想看公开内容，再使用 search_public_notes。
        4. 如果公开搜索结果中有明显相关笔记，可以继续调用 get_public_note_detail 获取详情。
        5. 如果工具没有找到可靠内容，可以再用通用知识回答，但必须说明”没有在笔记中找到足够来源”。

        可以为了完成一个用户请求连续调用多个工具。
        不要只因为用户表达模糊就直接放弃；应先进行一次合理的保守检索。
        如果多次检索仍无法判断用户意图，再向用户追问。
        """;

    private static final String SYSTEM_RAG = """
            可以使用 rag_search_my_notes 对当前用户自己的笔记进行语义检索。
            如果问题是语义性、学习性、概念性或不确定关键词的问题，优先使用 rag_search_my_notes。
            如果用户需要基于自己的知识内容回答问题，优先考虑 RAG 搜索或普通笔记搜索。
            如果 RAG 工具返回不可用，向用户说明该功能未启用或基础服务不可用。
            如果工具没有找到可靠内容，可以再用通用知识回答，但必须说明"没有在笔记中找到足够来源"。
            """;

    private static final String SYSTEM_PUBLIC = """
            可以搜索系统公开笔记，但只能访问公开可见内容。
            不要声称可以访问他人的私有笔记。
            可以通过 search_public_notes 搜索公开笔记，并通过 get_public_note_detail 获取公开笔记详情。
            """;

    private static final String SYSTEM_SOURCE_LINKS = """
            当回答内容引用了工具返回的笔记信息时，在相关句子或段落末尾添加来源链接。
            不同工具结果的 ID 字段不同，注意使用正确的字段：
            - 普通私有笔记工具（search_my_notes / get_my_note_detail / list_my_notes / list_my_published_notes）结果使用 id 字段：[《title》](kh-source://note/{id})
            - 公开笔记工具（search_public_notes / get_public_note_detail）结果使用 id 字段：[《title》](kh-source://public-note/{id})
            - RAG 语义检索 hit 结果使用 noteId 字段：[《title》](kh-source://note/{noteId})
            只能使用工具返回结果中真实存在的 id / noteId 和 title，不要编造。
            如果没有可靠的笔记来源，不要添加 kh-source 链接。
            不要把普通网页链接写成 kh-source 链接。
            不要输出裸 ID、[note:123] 或 [[note:123]] 等旧式标记。
            """;

    private static final String SYSTEM_OPERATIONS = """
            单篇发布/下架可以直接调用对应工具执行。
            批量下架公开笔记不能由你直接执行；必须先调用 prepare_batch_unpublish_published_notes 生成待确认操作，并交由用户在前端确认。
            只有用户完成前端确认后，系统才会执行批量下架。
            在确认完成前，不要声称已经完成批量下架。

            当用户要求根据文本、要点或主题创建笔记时，先整理为私有笔记草稿，并调用 prepare_create_private_note 生成待确认操作。
            只有用户完成前端确认后，系统才会创建私有笔记。
            在确认完成前，不要声称已经创建笔记。

            不要声称已经修改、删除笔记。
            不要声称支持批量删除、批量修改、管理员操作，或其他未列出的批量操作。
            """;

    static String buildSystemPrompt(boolean ragToolAvailable) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_BASE);
        sb.append('\n').append(SYSTEM_TOOL_STRATEGY);
        if (ragToolAvailable) {
            sb.append('\n').append(SYSTEM_RAG);
        }
        sb.append('\n').append(SYSTEM_PUBLIC);
        sb.append('\n').append(SYSTEM_OPERATIONS);
        sb.append('\n').append(SYSTEM_SOURCE_LINKS);
        return sb.toString();
    }

    private final ChatClient chatClient;
    private final AiAgentSessionService sessionService;
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final AiAgentActionEnvelopeParser actionEnvelopeParser = new AiAgentActionEnvelopeParser();

    public AiAgentChatService(ChatModel chatModel, AiAgentSessionService sessionService,
                              MessageChatMemoryAdvisor memoryAdvisor, boolean ragToolAvailable,
                              Object... tools) {
        this(chatModel, sessionService, memoryAdvisor, null, ragToolAvailable, tools);
    }

    public AiAgentChatService(ChatModel chatModel, AiAgentSessionService sessionService,
                              MessageChatMemoryAdvisor memoryAdvisor,
                              ToolCallAdvisor toolCallAdvisor,
                              boolean ragToolAvailable,
                              Object... tools) {
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService must not be null");
        this.memoryAdvisor = memoryAdvisor;
        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultSystem(buildSystemPrompt(ragToolAvailable));
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
