package com.yousheng.knowledgehub.ai.rag;

import com.yousheng.knowledgehub.ai.chat.AiChatClient;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchHit;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchResult;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchService;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiRagServiceTest {

    @Mock
    private AiIndexSearchService searchService;

    @Mock
    private AiChatClient chatClient;

    private AiRagService service;

    @BeforeEach
    void setUp() {
        service = new AiRagService(searchService, chatClient);
    }

    // --- 1. question null / blank ---
    @Test
    void ask_nullQuestion_throwsBizException() {
        assertThatThrownBy(() -> service.ask(null))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(biz.getMessage()).isEqualTo("问题不能为空");
                });
        verify(searchService, never()).search(any());
        verify(chatClient, never()).chat(any());
    }

    @Test
    void ask_blankQuestion_throwsBizException() {
        assertThatThrownBy(() -> service.ask("   "))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(biz.getMessage()).isEqualTo("问题不能为空");
                });
        verify(searchService, never()).search(any());
        verify(chatClient, never()).chat(any());
    }

    // --- 2. hits empty: fixed answer, no chatClient call ---
    @Test
    void ask_emptyHits_returnsFixedAnswerWithoutCallingChatClient() {
        when(searchService.search("hello")).thenReturn(
                new AiIndexSearchResult(1L, "hello", "gen-1", Collections.emptyList()));

        AiRagAnswer result = service.ask("hello");

        assertThat(result.answer()).isEqualTo("没有在你的笔记中找到足够相关的内容。");
        assertThat(result.sources()).isEmpty();
        verify(chatClient, never()).chat(any());
    }

    // --- 3. hits non-empty: calls chatClient, returns answer + sources ---
    @Test
    void ask_withHits_callsChatClientAndReturnsAnswerWithSources() {
        AiIndexSearchHit hit1 = new AiIndexSearchHit(10L, "Note A", "content of note A", 0, 0.92, "public", null);
        AiIndexSearchHit hit2 = new AiIndexSearchHit(20L, "Note B", "content of note B", 1, 0.85, "private", null);

        when(searchService.search("test question")).thenReturn(
                new AiIndexSearchResult(1L, "test question", "gen-1", List.of(hit1, hit2)));
        when(chatClient.chat(any())).thenReturn("LLM generated answer");

        AiRagAnswer result = service.ask("test question");

        assertThat(result.answer()).isEqualTo("LLM generated answer");
        assertThat(result.sources()).hasSize(2);

        AiRagSource source1 = result.sources().get(0);
        assertThat(source1.noteId()).isEqualTo(10L);
        assertThat(source1.title()).isEqualTo("Note A");
        assertThat(source1.snippet()).isEqualTo("content of note A");
        assertThat(source1.chunkIndex()).isEqualTo(0);
        assertThat(source1.distance()).isEqualTo(0.92);

        AiRagSource source2 = result.sources().get(1);
        assertThat(source2.noteId()).isEqualTo(20L);
        assertThat(source2.title()).isEqualTo("Note B");
        assertThat(source2.snippet()).isEqualTo("content of note B");
        assertThat(source2.chunkIndex()).isEqualTo(1);
        assertThat(source2.distance()).isEqualTo(0.85);

        verify(chatClient).chat(any());
    }

    // --- 4. prompt contains question and hit content ---
    @Test
    void ask_promptContainsQuestionAndHitContent() {
        AiIndexSearchHit hit = new AiIndexSearchHit(10L, "My Note", "the chunk content", 0, 0.92, "public", null);

        when(searchService.search("what is AI?")).thenReturn(
                new AiIndexSearchResult(1L, "what is AI?", "gen-1", List.of(hit)));
        when(chatClient.chat(any())).thenReturn("answer");

        service.ask("what is AI?");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(chatClient).chat(captor.capture());
        String prompt = captor.getValue();

        assertThat(prompt).contains("what is AI?");
        assertThat(prompt).contains("My Note");
        assertThat(prompt).contains("the chunk content");
    }

    // --- 5. prompt contains constraint phrases ---
    @Test
    void ask_promptContainsConstraintPhrases() {
        AiIndexSearchHit hit = new AiIndexSearchHit(10L, "T", "content", 0, 0.5, "public", null);

        when(searchService.search("q")).thenReturn(
                new AiIndexSearchResult(1L, "q", "gen-1", List.of(hit)));
        when(chatClient.chat(any())).thenReturn("ok");

        service.ask("q");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(chatClient).chat(captor.capture());
        String prompt = captor.getValue();

        assertThat(prompt).contains("仅根据下面提供的笔记片段回答");
        assertThat(prompt).contains("笔记中没有足够信息来回答这个问题");
    }

    // --- 6. trimmed question ---
    @Test
    void ask_trimsQuestionBeforeSearch() {
        AiIndexSearchHit hit = new AiIndexSearchHit(10L, "T", "content", 0, 0.5, "public", null);

        when(searchService.search("hello world")).thenReturn(
                new AiIndexSearchResult(1L, "hello world", "gen-1", List.of(hit)));
        when(chatClient.chat(any())).thenReturn("ok");

        service.ask("  hello world  ");

        verify(searchService).search("hello world");
    }

    // --- 7. chatClient throws ---
    @Test
    void ask_chatClientThrows_wrapsBizException() {
        AiIndexSearchHit hit = new AiIndexSearchHit(10L, "T", "content", 0, 0.5, "public", null);
        RuntimeException cause = new RuntimeException("LLM connection timeout");

        when(searchService.search("q")).thenReturn(
                new AiIndexSearchResult(1L, "q", "gen-1", List.of(hit)));
        when(chatClient.chat(any())).thenThrow(cause);

        assertThatThrownBy(() -> service.ask("q"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.AI_CHAT_SERVICE_UNAVAILABLE);
                    assertThat(biz.getCause()).isSameAs(cause);
                });
    }
}
