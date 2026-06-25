package com.yousheng.knowledgehub.ai.config;

import com.yousheng.knowledgehub.ai.agent.AiAgentChatService;
import com.yousheng.knowledgehub.ai.tool.note.NoteReadToolFacade;
import com.yousheng.knowledgehub.ai.tool.note.NoteReadTools;
import com.yousheng.knowledgehub.note.service.NoteService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "app.ai.agent", name = "enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
@ConditionalOnExpression("'${app.ai.chat.provider:deepseek}'.equals('deepseek') || '${app.ai.chat.provider:deepseek}'.equals('openai-compatible')")
public class AiAgentConfiguration {

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
    public AiAgentChatService aiAgentChatService(ChatModel chatModel, NoteReadTools noteReadTools) {
        return new AiAgentChatService(chatModel, noteReadTools);
    }
}
