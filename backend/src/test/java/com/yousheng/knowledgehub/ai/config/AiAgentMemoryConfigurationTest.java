package com.yousheng.knowledgehub.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yousheng.knowledgehub.note.service.NoteService;
import com.yousheng.knowledgehub.note.service.PublicNoteService;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiAgentMemoryConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class,
                    AiAgentConfiguration.class,
                    AiAgentMemoryConfiguration.class,
                    AiPropertiesConfiguration.class);

    @Test
    void shouldNotLoadMemoryBeansWhenMemoryDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=true",
                        "app.ai.agent.memory.enabled=false",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> {
                    assertThatThrownBy(() -> context.getBean(ChatMemoryRepository.class))
                            .isInstanceOf(NoSuchBeanDefinitionException.class);
                    assertThatThrownBy(() -> context.getBean(MessageWindowChatMemory.class))
                            .isInstanceOf(NoSuchBeanDefinitionException.class);
                    assertThatThrownBy(() -> context.getBean(MessageChatMemoryAdvisor.class))
                            .isInstanceOf(NoSuchBeanDefinitionException.class);
                });
    }

    @Test
    void shouldLoadMemoryBeansWhenMemoryEnabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=true",
                        "app.ai.agent.memory.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatMemoryRepository.class);
                    assertThat(context).hasSingleBean(MessageWindowChatMemory.class);
                    assertThat(context).hasSingleBean(MessageChatMemoryAdvisor.class);
                    assertThat(context.getBean(MessageChatMemoryAdvisor.class).getOrder())
                            .isEqualTo(AiAgentMemoryConfiguration.AGENT_MEMORY_ADVISOR_ORDER);
                });
    }

    @Test
    void shouldBackOffWhenCustomMemoryBeansExist() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class,
                        CustomMemoryConfig.class,
                        AiAgentConfiguration.class,
                        AiAgentMemoryConfiguration.class,
                        AiPropertiesConfiguration.class)
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=true",
                        "app.ai.agent.memory.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatMemoryRepository.class);
                    assertThat(context.getBean(ChatMemoryRepository.class))
                            .isSameAs(context.getBean("customChatMemoryRepository"));
                    assertThat(context).hasSingleBean(ChatMemory.class);
                    assertThat(context.getBean(ChatMemory.class))
                            .isSameAs(context.getBean("customChatMemory"));
                    assertThat(context).hasSingleBean(MessageChatMemoryAdvisor.class);
                    assertThat(context.getBean(MessageChatMemoryAdvisor.class))
                            .isSameAs(context.getBean("customMessageChatMemoryAdvisor"));
                });
    }

    @Test
    void shouldNotLoadMemoryBeansWhenAgentDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.ai.enabled=true",
                        "app.ai.agent.enabled=false",
                        "app.ai.agent.memory.enabled=true",
                        "spring.ai.model.chat=openai",
                        "app.ai.chat.provider=deepseek"
                )
                .run(context -> {
                    assertThatThrownBy(() -> context.getBean(ChatMemoryRepository.class))
                            .isInstanceOf(NoSuchBeanDefinitionException.class);
                    assertThatThrownBy(() -> context.getBean(MessageChatMemoryAdvisor.class))
                            .isInstanceOf(NoSuchBeanDefinitionException.class);
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
        PublicNoteService publicNoteService() {
            return Mockito.mock(PublicNoteService.class);
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
    static class CustomMemoryConfig {

        @Bean
        ChatMemoryRepository customChatMemoryRepository() {
            return Mockito.mock(ChatMemoryRepository.class);
        }

        @Bean
        ChatMemory customChatMemory() {
            return Mockito.mock(ChatMemory.class);
        }

        @Bean
        MessageChatMemoryAdvisor customMessageChatMemoryAdvisor(ChatMemory chatMemory) {
            return MessageChatMemoryAdvisor.builder(chatMemory).build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProperties.class)
    static class AiPropertiesConfiguration {
    }
}
