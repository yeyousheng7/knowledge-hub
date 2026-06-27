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
import com.yousheng.knowledgehub.ai.tool.note.PublicNoteToolFacade;
import com.yousheng.knowledgehub.ai.tool.note.PublicNoteTools;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchService;
import com.yousheng.knowledgehub.note.service.NoteService;
import com.yousheng.knowledgehub.note.service.PublicNoteService;
import com.yousheng.knowledgehub.security.CurrentUserPrincipal;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AiAgentConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class, AiAgentConfiguration.class, AiPropertiesConfiguration.class);

    @Test
    void shouldNotLoadAgentBeansWhenAiDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=false",
                        "app.ai.agent.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NoteReadToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteReadTools.class);
                    assertThat(context).doesNotHaveBean(NoteWriteToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteWriteTools.class);
                    assertThat(context).doesNotHaveBean(NoteActionToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteActionTools.class);
                    assertThat(context).doesNotHaveBean(PublicNoteToolFacade.class);
                    assertThat(context).doesNotHaveBean(PublicNoteTools.class);
                    assertThat(context).doesNotHaveBean(AiAgentSessionService.class);
                    assertThat(context).doesNotHaveBean(AiAgentOperationConfirmService.class);
                    assertThat(context).doesNotHaveBean(AiAgentChatService.class);
                });
    }

    @Test
    void shouldNotLoadAgentBeansWhenAgentDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=false",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NoteReadToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteReadTools.class);
                    assertThat(context).doesNotHaveBean(NoteWriteToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteWriteTools.class);
                    assertThat(context).doesNotHaveBean(NoteActionToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteActionTools.class);
                    assertThat(context).doesNotHaveBean(PublicNoteToolFacade.class);
                    assertThat(context).doesNotHaveBean(PublicNoteTools.class);
                    assertThat(context).doesNotHaveBean(AiAgentSessionService.class);
                    assertThat(context).doesNotHaveBean(AiAgentOperationConfirmService.class);
                    assertThat(context).doesNotHaveBean(AiAgentChatService.class);
                });
    }

    @Test
    void shouldNotLoadAgentBeansWhenChatModelIsNone() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=true",
                        "spring.ai.model.chat=none",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NoteReadToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteReadTools.class);
                    assertThat(context).doesNotHaveBean(NoteWriteToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteWriteTools.class);
                    assertThat(context).doesNotHaveBean(NoteActionToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteActionTools.class);
                    assertThat(context).doesNotHaveBean(PublicNoteToolFacade.class);
                    assertThat(context).doesNotHaveBean(PublicNoteTools.class);
                    assertThat(context).doesNotHaveBean(AiAgentSessionService.class);
                    assertThat(context).doesNotHaveBean(AiAgentOperationConfirmService.class);
                    assertThat(context).doesNotHaveBean(AiAgentChatService.class);
                });
    }

    @Test
    void shouldNotLoadAgentBeansWhenProviderMismatch() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=unknown"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NoteReadToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteReadTools.class);
                    assertThat(context).doesNotHaveBean(NoteWriteToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteWriteTools.class);
                    assertThat(context).doesNotHaveBean(NoteActionToolFacade.class);
                    assertThat(context).doesNotHaveBean(NoteActionTools.class);
                    assertThat(context).doesNotHaveBean(PublicNoteToolFacade.class);
                    assertThat(context).doesNotHaveBean(PublicNoteTools.class);
                    assertThat(context).doesNotHaveBean(AiAgentSessionService.class);
                    assertThat(context).doesNotHaveBean(AiAgentOperationConfirmService.class);
                    assertThat(context).doesNotHaveBean(AiAgentChatService.class);
                });
    }

    @Test
    void shouldLoadAgentBeansWhenConditionsMatchDeepSeek() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(NoteReadToolFacade.class);
                    assertThat(context).hasSingleBean(NoteReadTools.class);
                    assertThat(context).hasSingleBean(NoteWriteToolFacade.class);
                    assertThat(context).hasSingleBean(NoteWriteTools.class);
                    assertThat(context).hasSingleBean(PublicNoteToolFacade.class);
                    assertThat(context).hasSingleBean(PublicNoteTools.class);
                    assertThat(context).hasSingleBean(AiAgentPendingOperationStore.class);
                    assertThat(context).hasSingleBean(NoteActionToolFacade.class);
                    assertThat(context).hasSingleBean(NoteActionTools.class);
                    assertThat(context).hasSingleBean(ToolCallAdvisor.class);
                    assertThat(context.getBean(ToolCallAdvisor.class).getOrder())
                            .isEqualTo(AiAgentConfiguration.AGENT_TOOL_CALL_ADVISOR_ORDER);
                    assertThat(context).hasSingleBean(AiAgentSessionService.class);
                    assertThat(context).hasSingleBean(AiAgentOperationConfirmService.class);
                    assertThat(context).hasSingleBean(AiAgentChatService.class);
                });
    }

    @Test
    void shouldLoadAgentBeansWhenConditionsMatchOpenAiCompatible() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=openai-compatible"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(NoteReadToolFacade.class);
                    assertThat(context).hasSingleBean(NoteReadTools.class);
                    assertThat(context).hasSingleBean(NoteWriteToolFacade.class);
                    assertThat(context).hasSingleBean(NoteWriteTools.class);
                    assertThat(context).hasSingleBean(PublicNoteToolFacade.class);
                    assertThat(context).hasSingleBean(PublicNoteTools.class);
                    assertThat(context).hasSingleBean(NoteActionTools.class);
                    assertThat(context).hasSingleBean(AiAgentSessionService.class);
                    assertThat(context).hasSingleBean(AiAgentOperationConfirmService.class);
                    assertThat(context).hasSingleBean(AiAgentChatService.class);
                });
    }

    @Test
    void agentPromptIncludesRagToolWhenRagEnabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=true",
                        "app.ai.rag.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> {
                    var chatModel = (CapturingChatModel) context.getBean(ChatModel.class);
                    var service = context.getBean(AiAgentChatService.class);
                    setupUserContext(context);

                    try {
                        service.chat("test query");

                        Prompt prompt = chatModel.lastPrompt();
                        assertThat(prompt).isNotNull();

                        List<String> toolNames = toolNamesFromPrompt(prompt);
                        assertThat(toolNames).contains("rag_search_my_notes");

                        String systemText = prompt.getSystemMessage().getText();
                        assertThat(systemText).contains("rag_search_my_notes");
                        assertThat(systemText).contains("kh-source://note/{id}");
                        assertThat(systemText).contains("kh-source://public-note/{id}");
                        assertThat(systemText).contains("kh-source://note/{noteId}");
                        assertThat(systemText).contains("使用 id 字段");
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
    }

    @Test
    void agentPromptExcludesRagToolWhenRagDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=true",
                        "app.ai.rag.enabled=false",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> {
                    var chatModel = (CapturingChatModel) context.getBean(ChatModel.class);
                    var service = context.getBean(AiAgentChatService.class);
                    setupUserContext(context);

                    try {
                        service.chat("test query");

                        Prompt prompt = chatModel.lastPrompt();
                        assertThat(prompt).isNotNull();

                        List<String> toolNames = toolNamesFromPrompt(prompt);
                        assertThat(toolNames).doesNotContain("rag_search_my_notes");

                        String systemText = prompt.getSystemMessage().getText();
                        assertThat(systemText).doesNotContain("rag_search_my_notes");
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        ChatModel chatModel() {
            return new CapturingChatModel();
        }

        @Bean
        NoteService noteService() {
            return Mockito.mock(NoteService.class);
        }

        @Bean
        PublicNoteService publicNoteService() {
            return Mockito.mock(PublicNoteService.class);
        }

        @Bean
        AppUserMapper appUserMapper() {
            return Mockito.mock(AppUserMapper.class);
        }

        @Bean
        AiIndexSearchService aiIndexSearchService() {
            return Mockito.mock(AiIndexSearchService.class);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return Mockito.mock(StringRedisTemplate.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProperties.class)
    static class AiPropertiesConfiguration {
    }

    static class CapturingChatModel implements ChatModel {

        private Prompt lastPrompt;

        @Override
        public ChatResponse call(Prompt prompt) {
            this.lastPrompt = prompt;
            return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
        }

        Prompt lastPrompt() {
            return lastPrompt;
        }
    }

    private static void setupUserContext(org.springframework.context.ApplicationContext context) {
        var mapper = context.getBean(AppUserMapper.class);
        when(mapper.selectById(1L)).thenReturn(enabledUser());

        var auth = new TestingAuthenticationToken(
                new CurrentUserPrincipal(1L, "testuser", "USER"), null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static List<String> toolNamesFromPrompt(Prompt prompt) {
        ChatOptions options = prompt.getOptions();
        if (options instanceof ToolCallingChatOptions toolOptions) {
            return toolOptions.getToolCallbacks().stream()
                    .map(tc -> tc.getToolDefinition().name())
                    .toList();
        }
        return List.of();
    }

    private static AppUser enabledUser() {
        var user = new AppUser();
        user.setId(1L);
        user.setStatus(UserStatus.ENABLED.name());
        return user;
    }
}
