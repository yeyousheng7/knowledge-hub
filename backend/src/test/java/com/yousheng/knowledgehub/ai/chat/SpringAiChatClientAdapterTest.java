package com.yousheng.knowledgehub.ai.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiChatClientAdapterTest {

    @Mock
    private ChatModel chatModel;

    // --- 1. successful call ---
    @Test
    void chat_returnsModelResponseText() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("LLM answer")))));

        SpringAiChatClientAdapter adapter = new SpringAiChatClientAdapter(chatModel);
        String result = adapter.chat("test prompt");

        assertThat(result).isEqualTo("LLM answer");
    }

    // --- 2. prompt is passed as user message ---
    @Test
    void chat_passesPromptAsUserMessage() {
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));

        new SpringAiChatClientAdapter(chatModel).chat("what is AI?");

        verify(chatModel).call(captor.capture());
        Prompt prompt = captor.getValue();
        assertThat(prompt.getUserMessage().getText()).contains("what is AI?");
    }

    // --- 3. system message is set ---
    @Test
    void chat_includesSystemMessage() {
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));

        new SpringAiChatClientAdapter(chatModel).chat("q");

        verify(chatModel).call(captor.capture());
        Prompt prompt = captor.getValue();
        assertThat(prompt.getSystemMessage()).isNotNull();
        assertThat(prompt.getSystemMessage().getText()).contains("KnowledgeHub");
        assertThat(prompt.getSystemMessage().getText()).contains("知识库助手");
    }

    // --- 4. null / blank prompt ---
    @Test
    void chat_nullPrompt_throwsIllegalArgumentException() {
        SpringAiChatClientAdapter adapter = new SpringAiChatClientAdapter(chatModel);

        assertThatThrownBy(() -> adapter.chat(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prompt must not be blank");
    }

    @Test
    void chat_blankPrompt_throwsIllegalArgumentException() {
        SpringAiChatClientAdapter adapter = new SpringAiChatClientAdapter(chatModel);

        assertThatThrownBy(() -> adapter.chat("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prompt must not be blank");
    }

    // --- 5. ChatModel throws → adapter does not swallow ---
    @Test
    void chat_chatModelThrows_propagatesException() {
        RuntimeException cause = new RuntimeException("connection timeout");
        when(chatModel.call(any(Prompt.class))).thenThrow(cause);

        SpringAiChatClientAdapter adapter = new SpringAiChatClientAdapter(chatModel);

        assertThatThrownBy(() -> adapter.chat("test"))
                .isInstanceOf(RuntimeException.class)
                .isSameAs(cause);
    }

    // --- 6. Spring AI returns empty content ---
    @Test
    void chat_emptyContent_throwsRuntimeException() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("")))));

        SpringAiChatClientAdapter adapter = new SpringAiChatClientAdapter(chatModel);

        assertThatThrownBy(() -> adapter.chat("test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("empty content");
    }

    @Test
    void chat_noGenerations_throwsRuntimeException() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(Collections.emptyList()));

        SpringAiChatClientAdapter adapter = new SpringAiChatClientAdapter(chatModel);

        assertThatThrownBy(() -> adapter.chat("test"))
                .isInstanceOf(RuntimeException.class);
    }
}
