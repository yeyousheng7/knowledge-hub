package com.yousheng.knowledgehub.ai.agent;

import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiAgentChatServiceTest {

    @Test
    void blankMessage_throwsException() {
        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel("response"));

        assertThatThrownBy(() -> service.chat("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void nullMessage_throwsException() {
        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel("response"));

        assertThatThrownBy(() -> service.chat(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void validMessage_returnsModelContent() {
        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel("Hello from AI!"));

        String result = service.chat("Hi there");

        assertThat(result).isEqualTo("Hello from AI!");
    }

    @Test
    void modelReturnsNullContent_throwsBizException() {
        AiAgentChatService service = new AiAgentChatService(
                new FakeChatModel(null));

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
                new FakeChatModel("   "));

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
        AiAgentChatService service = new AiAgentChatService(throwingModel);

        assertThatThrownBy(() -> service.chat("Hi"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizEx = (BizException) ex;
                    assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.AI_CHAT_SERVICE_UNAVAILABLE);
                    assertThat(bizEx.getCause()).isSameAs(failure);
                });
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
}
