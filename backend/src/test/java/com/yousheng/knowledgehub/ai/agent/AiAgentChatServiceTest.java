package com.yousheng.knowledgehub.ai.agent;

import com.yousheng.knowledgehub.ai.agent.dto.AiAgentChatResponse;
import com.yousheng.knowledgehub.ai.tool.demo.DemoActionTools;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.CurrentUserPrincipal;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiAgentChatServiceTest {

    private AppUserMapper appUserMapper;

    @BeforeEach
    void setUp() {
        appUserMapper = mock(AppUserMapper.class);
        when(appUserMapper.selectById(1L)).thenReturn(user(UserStatus.ENABLED.name()));

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                new CurrentUserPrincipal(1L, "testuser", "USER"), null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AiAgentSessionService sessionService() {
        return new AiAgentSessionService(null, appUserMapper);
    }

    private AiAgentSessionService sessionService(ChatMemory chatMemory) {
        return new AiAgentSessionService(chatMemory, appUserMapper);
    }

    @Test
    void blankMessage_throwsException() {
        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel("response"), sessionService(), null);

        assertThatThrownBy(() -> service.chat("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void nullMessage_throwsException() {
        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel("response"), sessionService(), null);

        assertThatThrownBy(() -> service.chat(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void validMessage_returnsModelContent() {
        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel("Hello from AI!"), sessionService(), null);

        AiAgentChatResponse result = service.chat("Hi there");

        assertThat(result.answer()).isEqualTo("Hello from AI!");
        assertThat(result.actions()).isEmpty();
    }

    @Test
    void demoActionTool_isReturnDirect() throws Exception {
        Tool tool = DemoActionTools.class
                .getMethod("prepareDemoAction", String.class)
                .getAnnotation(Tool.class);

        assertThat(tool.returnDirect()).isTrue();
    }

    @Test
    void actionEnvelopeContent_parsesActions() {
        String toolResult = new DemoActionTools().prepareDemoAction("Spike preview");
        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel(toolResult), sessionService(), null);

        AiAgentChatResponse result = service.chat("demo structured action");

        assertThat(result.answer()).isEqualTo("Demo action prepared. No real business operation was executed.");
        assertThat(result.actions()).hasSize(1);
        assertThat(result.actions().get(0).type()).isEqualTo("DEMO_ACTION");
        assertThat(result.actions().get(0).payload()).containsEntry("source", "returnDirect-spike");
    }

    @Test
    void modelReturnsNullContent_throwsBizException() {
        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel(null), sessionService(), null);

        assertThatThrownBy(() -> service.chat("Hi"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizEx = (BizException) ex;
                    assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.AI_CHAT_SERVICE_UNAVAILABLE);
                });
    }

    @Test
    void modelReturnsBlankContent_throwsBizException() {
        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel("   "), sessionService(), null);

        assertThatThrownBy(() -> service.chat("Hi"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizEx = (BizException) ex;
                    assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.AI_CHAT_SERVICE_UNAVAILABLE);
                });
    }

    @Test
    void modelThrowsRuntimeException_wrapsAsBizException() {
        RuntimeException failure = new RuntimeException("upstream failure");
        ChatModel throwingModel = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw failure;
            }
        };
        AiAgentChatService service = new AiAgentChatService(throwingModel, sessionService(), null);

        assertThatThrownBy(() -> service.chat("Hi"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizEx = (BizException) ex;
                    assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.AI_CHAT_SERVICE_UNAVAILABLE);
                    assertThat(bizEx.getCause()).isSameAs(failure);
                });
    }

    @Test
    void memoryDisabled_keepsSingleTurnBehavior() {
        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel("single turn"), sessionService(), null);

        AiAgentChatResponse result = service.chat("query");

        assertThat(result.answer()).isEqualTo("single turn");
        assertThat(result.actions()).isEmpty();
    }

    @Test
    void disabledUser_throwsUserDisabledBeforeCallingModel() {
        when(appUserMapper.selectById(1L)).thenReturn(user(UserStatus.DISABLED.name()));
        TrackingChatModel chatModel = new TrackingChatModel("unused");
        AiAgentChatService service = new AiAgentChatService(chatModel, sessionService(), null);

        assertThatThrownBy(() -> service.chat("query"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizEx = (BizException) ex;
                    assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.USER_DISABLED);
                });
        assertThat(chatModel.called()).isFalse();
    }

    @Test
    void memoryEnabled_passesConversationIdParam() {
        InMemoryChatMemoryRepository repo = new InMemoryChatMemoryRepository();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo).maxMessages(20).build();
        MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        AiAgentSessionService sessionService = sessionService(chatMemory);

        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel("multi turn"), sessionService, advisor);

        AiAgentChatResponse result = service.chat("first question");

        assertThat(result.answer()).isEqualTo("multi turn");
        assertThat(repo.findByConversationId("kh:ai:agent:session:1:current")).isNotEmpty();
    }

    private static class FakeChatModel implements ChatModel {

        private final String response;

        FakeChatModel(String response) {
            this.response = response;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
        }
    }

    private static class TrackingChatModel extends FakeChatModel {

        private boolean called;

        TrackingChatModel(String response) {
            super(response);
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            called = true;
            return super.call(prompt);
        }

        boolean called() {
            return called;
        }
    }

    private static AppUser user(String status) {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setStatus(status);
        return user;
    }
}
