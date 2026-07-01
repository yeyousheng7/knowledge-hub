package com.yousheng.knowledgehub.ai.tool.note;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yousheng.knowledgehub.ai.agent.AiAgentActionEnvelopeParser;
import com.yousheng.knowledgehub.ai.agent.AiAgentSessionService;
import com.yousheng.knowledgehub.ai.agent.dto.AiAgentChatResponse;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperationStore;
import com.yousheng.knowledgehub.ai.config.AiProperties;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteListItemResponse;
import com.yousheng.knowledgehub.note.service.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteActionToolFacadeTest {

    private static final Instant NOW = Instant.parse("2026-06-26T12:00:00Z");

    @Mock
    private NoteService noteService;

    @Mock
    private AiAgentSessionService sessionService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private AiAgentActionEnvelopeParser parser;
    private NoteActionToolFacade facade;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        parser = new AiAgentActionEnvelopeParser();
        AiProperties aiProperties = new AiProperties();
        aiProperties.getAgent().getOperation().setTtlMinutes(30);
        AiAgentPendingOperationStore operationStore =
                new AiAgentPendingOperationStore(stringRedisTemplate, objectMapper);
        facade = new NoteActionToolFacade(
                noteService,
                sessionService,
                operationStore,
                aiProperties,
                objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void prepareBatchUnpublishPublishedNotes_withSelectedNotes_returnsPendingOperationActionAndStoresExactIds()
            throws Exception {
        when(sessionService.requireCurrentEnabledUserId()).thenReturn(7L);
        when(noteService.getMyPublishedNotesForBatchUnpublish(List.of(2L, 1L)))
                .thenReturn(List.of(note(2L, "second"), note(1L, "first")));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String content = facade.prepareBatchUnpublishPublishedNotes(List.of(2L, 1L));

        AiAgentChatResponse response = parser.parse(content);
        assertThat(response.actions()).hasSize(1);
        assertThat(response.actions().get(0).type()).isEqualTo("PENDING_OPERATION");
        assertThat(response.actions().get(0).payload())
                .containsEntry("operationType", "BATCH_UNPUBLISH_NOTES");
        assertThat((List<?>) response.actions().get(0).payload().get("affectedItems")).hasSize(2);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());
        assertThat(keyCaptor.getValue()).startsWith("ai:operation:7:");
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(30));

        JsonNode stored = objectMapper.readTree(valueCaptor.getValue());
        assertThat(stored.get("operationType").asText()).isEqualTo("BATCH_UNPUBLISH_NOTES");
        assertThat(stored.get("userId").asLong()).isEqualTo(7L);
        assertThat(stored.get("status").asText()).isEqualTo("PENDING");
        assertThat(stored.get("noteIds").get(0).asLong()).isEqualTo(2L);
        assertThat(stored.get("noteIds").get(1).asLong()).isEqualTo(1L);
        verify(noteService).getMyPublishedNotesForBatchUnpublish(List.of(2L, 1L));
    }

    @Test
    void prepareBatchUnpublishPublishedNotes_withInvalidSelection_returnsNoActionAndDoesNotStoreOperation() {
        when(sessionService.requireCurrentEnabledUserId()).thenReturn(7L);
        when(noteService.getMyPublishedNotesForBatchUnpublish(List.of(99L)))
                .thenThrow(new BizException(ErrorCode.NOTE_NOT_FOUND, "待下架笔记当前不可操作"));

        AiAgentChatResponse response = parser.parse(facade.prepareBatchUnpublishPublishedNotes(List.of(99L)));

        assertThat(response.answer()).contains("当前不可操作");
        assertThat(response.actions()).isEmpty();
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void prepareBatchUnpublishPublishedNotes_disabledUserDoesNotCreateOperation() {
        when(sessionService.requireCurrentEnabledUserId())
                .thenThrow(new BizException(ErrorCode.USER_DISABLED));

        assertThatThrownBy(() -> facade.prepareBatchUnpublishPublishedNotes(List.of(1L)))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.USER_DISABLED));
        verifyNoInteractions(noteService, stringRedisTemplate);
    }

    @Test
    void prepareCreatePrivateNote_withValidDraft_returnsPendingOperationActionAndStoresOperation()
            throws Exception {
        when(sessionService.requireCurrentEnabledUserId()).thenReturn(7L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String content = facade.prepareCreatePrivateNote(
                "  Draft title  ",
                "  # Draft\ncontent  ",
                "  draft summary  ",
                List.of(" java ", "ai", "java"));

        AiAgentChatResponse response = parser.parse(content);
        assertThat(response.actions()).hasSize(1);
        assertThat(response.actions().get(0).type()).isEqualTo("PENDING_OPERATION");
        assertThat(response.actions().get(0).payload())
                .containsEntry("operationType", "CREATE_PRIVATE_NOTE");
        @SuppressWarnings("unchecked")
        Map<String, Object> draft = (Map<String, Object>) response.actions().get(0).payload().get("draft");
        assertThat(draft)
                .containsEntry("title", "Draft title")
                .containsEntry("summary", "draft summary")
                .containsEntry("contentMd", "# Draft\ncontent");
        assertThat(draft.get("recommendedTags")).isEqualTo(List.of("java", "ai"));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(),
                valueCaptor.capture(),
                ttlCaptor.capture());

        assertThat(keyCaptor.getValue()).startsWith("ai:operation:7:");
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(30));
        JsonNode stored = objectMapper.readTree(valueCaptor.getValue());
        assertThat(stored.get("operationType").asText()).isEqualTo("CREATE_PRIVATE_NOTE");
        assertThat(stored.get("userId").asLong()).isEqualTo(7L);
        assertThat(stored.get("status").asText()).isEqualTo("PENDING");
        assertThat(stored.get("noteIds")).isEmpty();
        assertThat(stored.get("payload").get("title").asText()).isEqualTo("Draft title");
        verifyNoInteractions(noteService);
    }

    @Test
    void prepareCreatePrivateNote_withBlankTitle_returnsNoActionAndDoesNotStoreOperation() {
        when(sessionService.requireCurrentEnabledUserId()).thenReturn(7L);

        AiAgentChatResponse response = parser.parse(facade.prepareCreatePrivateNote(
                " ",
                "content",
                null,
                List.of("ai")));

        assertThat(response.answer()).contains("标题不能为空");
        assertThat(response.actions()).isEmpty();
        verifyNoInteractions(noteService, stringRedisTemplate);
    }

    private NoteListItemResponse note(Long id, String title) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 26, 20, 0);
        return new NoteListItemResponse(
                id,
                title,
                "summary",
                null,
                List.of(),
                "PUBLIC",
                "NORMAL",
                now,
                now,
                now);
    }
}
