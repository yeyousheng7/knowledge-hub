package com.yousheng.knowledgehub.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yousheng.knowledgehub.ai.agent.AiAgentChatService;
import com.yousheng.knowledgehub.ai.agent.AiAgentSessionService;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperationStore;
import com.yousheng.knowledgehub.ai.tool.note.NoteActionToolFacade;
import com.yousheng.knowledgehub.ai.tool.note.NoteActionTools;
import com.yousheng.knowledgehub.ai.tool.note.NoteReadToolFacade;
import com.yousheng.knowledgehub.ai.tool.note.NoteReadTools;
import com.yousheng.knowledgehub.ai.tool.note.NoteWriteToolFacade;
import com.yousheng.knowledgehub.ai.tool.note.NoteWriteTools;
import com.yousheng.knowledgehub.note.service.NoteService;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

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
                    assertThat(context).doesNotHaveBean(AiAgentSessionService.class);
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
                    assertThat(context).doesNotHaveBean(AiAgentSessionService.class);
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
                    assertThat(context).doesNotHaveBean(AiAgentSessionService.class);
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
                    assertThat(context).doesNotHaveBean(AiAgentSessionService.class);
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
                    assertThat(context).hasSingleBean(AiAgentPendingOperationStore.class);
                    assertThat(context).hasSingleBean(NoteActionToolFacade.class);
                    assertThat(context).hasSingleBean(NoteActionTools.class);
                    assertThat(context).hasSingleBean(ToolCallAdvisor.class);
                    assertThat(context.getBean(ToolCallAdvisor.class).getOrder())
                            .isEqualTo(AiAgentConfiguration.AGENT_TOOL_CALL_ADVISOR_ORDER);
                    assertThat(context).hasSingleBean(AiAgentSessionService.class);
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
                    assertThat(context).hasSingleBean(NoteActionTools.class);
                    assertThat(context).hasSingleBean(AiAgentSessionService.class);
                    assertThat(context).hasSingleBean(AiAgentChatService.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        ChatModel chatModel() {
            return Mockito.mock(ChatModel.class);
        }

        @Bean
        NoteService noteService() {
            return Mockito.mock(NoteService.class);
        }

        @Bean
        AppUserMapper appUserMapper() {
            return Mockito.mock(AppUserMapper.class);
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
}
