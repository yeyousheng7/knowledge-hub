package com.yousheng.knowledgehub.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yousheng.knowledgehub.ai.agent.AiAgentChatService;
import com.yousheng.knowledgehub.ai.agent.AiAgentOperationConfirmService;
import com.yousheng.knowledgehub.ai.agent.AiAgentSessionService;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperationStore;
import com.yousheng.knowledgehub.ai.tool.note.NoteActionToolFacade;
import com.yousheng.knowledgehub.ai.tool.note.NoteActionTools;
import com.yousheng.knowledgehub.ai.tool.note.NoteReadToolFacade;
import com.yousheng.knowledgehub.ai.tool.note.NoteReadTools;
import com.yousheng.knowledgehub.ai.tool.note.NoteWriteToolFacade;
import com.yousheng.knowledgehub.ai.tool.note.NoteWriteTools;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchService;
import com.yousheng.knowledgehub.ai.tool.note.PublicNoteToolFacade;
import com.yousheng.knowledgehub.ai.tool.note.PublicNoteTools;
import com.yousheng.knowledgehub.ai.tool.rag.RagNoteToolFacade;
import com.yousheng.knowledgehub.ai.tool.rag.RagNoteTools;
import com.yousheng.knowledgehub.note.service.NoteService;
import com.yousheng.knowledgehub.note.service.PublicNoteService;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "app.ai.agent", name = "enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
@ConditionalOnExpression("'${app.ai.chat.provider:deepseek}'.equals('deepseek') || '${app.ai.chat.provider:deepseek}'.equals('openai-compatible')")
public class AiAgentConfiguration {

    static final int AGENT_TOOL_CALL_ADVISOR_ORDER = Ordered.HIGHEST_PRECEDENCE + 300;

    @Bean
    @ConditionalOnMissingBean
    public NoteReadToolFacade noteReadToolFacade(NoteService noteService) {
        return new NoteReadToolFacade(noteService);
    }

    @Bean
    @ConditionalOnMissingBean
    public NoteReadTools noteReadTools(NoteReadToolFacade noteReadToolFacade) {
        return new NoteReadTools(noteReadToolFacade);
    }

    @Bean
    @ConditionalOnMissingBean
    public NoteWriteToolFacade noteWriteToolFacade(NoteService noteService) {
        return new NoteWriteToolFacade(noteService);
    }

    @Bean
    @ConditionalOnMissingBean
    public NoteWriteTools noteWriteTools(NoteWriteToolFacade noteWriteToolFacade) {
        return new NoteWriteTools(noteWriteToolFacade);
    }

    @Bean
    @ConditionalOnMissingBean
    public PublicNoteToolFacade publicNoteToolFacade(PublicNoteService publicNoteService) {
        return new PublicNoteToolFacade(publicNoteService);
    }

    @Bean
    @ConditionalOnMissingBean
    public PublicNoteTools publicNoteTools(PublicNoteToolFacade publicNoteToolFacade) {
        return new PublicNoteTools(publicNoteToolFacade);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "app.ai.rag", name = "enabled", havingValue = "true")
    public RagNoteToolFacade ragNoteToolFacade(ObjectProvider<AiIndexSearchService> searchServiceProvider,
                                               AiProperties aiProperties) {
        return new RagNoteToolFacade(searchServiceProvider, aiProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "app.ai.rag", name = "enabled", havingValue = "true")
    public RagNoteTools ragNoteTools(RagNoteToolFacade ragNoteToolFacade) {
        return new RagNoteTools(ragNoteToolFacade);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiAgentPendingOperationStore aiAgentPendingOperationStore(StringRedisTemplate stringRedisTemplate,
                                                                     ObjectMapper objectMapper) {
        return new AiAgentPendingOperationStore(stringRedisTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public NoteActionToolFacade noteActionToolFacade(NoteService noteService,
                                                     AiAgentSessionService sessionService,
                                                     AiAgentPendingOperationStore operationStore,
                                                     AiProperties aiProperties,
                                                     ObjectMapper objectMapper) {
        return new NoteActionToolFacade(noteService, sessionService, operationStore, aiProperties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public NoteActionTools noteActionTools(NoteActionToolFacade noteActionToolFacade) {
        return new NoteActionTools(noteActionToolFacade);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiAgentOperationConfirmService aiAgentOperationConfirmService(
            AiAgentSessionService sessionService,
            AiAgentPendingOperationStore operationStore,
            NoteService noteService) {
        return new AiAgentOperationConfirmService(sessionService, operationStore, noteService);
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallAdvisor.class)
    public ToolCallAdvisor agentToolCallAdvisor() {
        return ToolCallAdvisor.builder()
                .advisorOrder(AGENT_TOOL_CALL_ADVISOR_ORDER)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public AiAgentSessionService aiAgentSessionService(
            ObjectProvider<ChatMemory> chatMemoryProvider,
            AppUserMapper appUserMapper) {
        return new AiAgentSessionService(
                chatMemoryProvider.getIfAvailable(),
                appUserMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiAgentChatService aiAgentChatService(ChatModel chatModel,
                                                 AiAgentSessionService sessionService,
                                                 ObjectProvider<MessageChatMemoryAdvisor> advisorProvider,
                                                 ToolCallAdvisor toolCallAdvisor,
                                                 NoteReadTools noteReadTools,
                                                 NoteWriteTools noteWriteTools,
                                                 NoteActionTools noteActionTools,
                                                 PublicNoteTools publicNoteTools,
                                                 ObjectProvider<RagNoteTools> ragNoteToolsProvider) {
        List<Object> tools = new ArrayList<>();
        tools.add(noteReadTools);
        tools.add(noteWriteTools);
        tools.add(noteActionTools);
        tools.add(publicNoteTools);
        boolean ragToolAvailable = false;
        RagNoteTools ragNoteTools = ragNoteToolsProvider.getIfAvailable();
        if (ragNoteTools != null) {
            tools.add(ragNoteTools);
            ragToolAvailable = true;
        }
        return new AiAgentChatService(chatModel, sessionService, advisorProvider.getIfAvailable(), toolCallAdvisor,
                ragToolAvailable, tools.toArray());
    }
}
