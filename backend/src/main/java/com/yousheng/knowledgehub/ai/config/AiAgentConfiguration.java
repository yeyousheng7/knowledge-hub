package com.yousheng.knowledgehub.ai.config;

import com.yousheng.knowledgehub.ai.agent.AiAgentChatService;
import com.yousheng.knowledgehub.ai.agent.AiAgentSessionService;
import com.yousheng.knowledgehub.ai.tool.demo.DemoActionTools;
import com.yousheng.knowledgehub.ai.tool.note.NoteReadToolFacade;
import com.yousheng.knowledgehub.ai.tool.note.NoteReadTools;
import com.yousheng.knowledgehub.ai.tool.note.NoteWriteToolFacade;
import com.yousheng.knowledgehub.ai.tool.note.NoteWriteTools;
import com.yousheng.knowledgehub.note.service.NoteService;
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
    public DemoActionTools demoActionTools() {
        return new DemoActionTools();
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallAdvisor.class)
    public ToolCallAdvisor agentToolCallAdvisor() {
        return ToolCallAdvisor.builder()
                .advisorOrder(AGENT_TOOL_CALL_ADVISOR_ORDER)
                .disableMemory()
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
                                                 DemoActionTools demoActionTools) {
        return new AiAgentChatService(chatModel, sessionService, advisorProvider.getIfAvailable(), toolCallAdvisor,
                noteReadTools, noteWriteTools, demoActionTools);
    }
}
