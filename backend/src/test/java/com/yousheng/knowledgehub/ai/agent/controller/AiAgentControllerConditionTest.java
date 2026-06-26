package com.yousheng.knowledgehub.ai.agent.controller;

import com.yousheng.knowledgehub.ai.agent.AiAgentChatService;
import com.yousheng.knowledgehub.ai.agent.AiAgentSessionService;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiAgentControllerConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiAgentController.class)
            .withBean(AiAgentChatService.class, () -> mock(AiAgentChatService.class))
            .withBean(AiAgentSessionService.class,
                    () -> new AiAgentSessionService(null, mock(AppUserMapper.class)));

    @Test
    void shouldNotCreateControllerWhenAiDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=false",
                        "app.ai.agent.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiAgentController.class));
    }

    @Test
    void shouldNotCreateControllerWhenAgentDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=false",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiAgentController.class));
    }

    @Test
    void shouldNotCreateControllerWhenChatModelIsNone() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=true",
                        "spring.ai.model.chat=none",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> assertThat(context).doesNotHaveBean(AiAgentController.class));
    }

    @Test
    void shouldCreateControllerWhenConditionsMatch() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> assertThat(context).hasSingleBean(AiAgentController.class));
    }
}
