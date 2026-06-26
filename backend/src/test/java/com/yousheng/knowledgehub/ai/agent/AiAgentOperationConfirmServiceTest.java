package com.yousheng.knowledgehub.ai.agent;

import com.yousheng.knowledgehub.ai.agent.dto.AiAgentOperationConfirmResponse;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperation;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperationStore;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteBatchUnpublishResult;
import com.yousheng.knowledgehub.note.dto.NoteListItemResponse;
import com.yousheng.knowledgehub.note.service.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiAgentOperationConfirmServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-26T12:00:00Z");

    private AiAgentSessionService sessionService;
    private AiAgentPendingOperationStore operationStore;
    private NoteService noteService;
    private AiAgentOperationConfirmService service;

    @BeforeEach
    void setUp() {
        sessionService = mock(AiAgentSessionService.class);
        operationStore = mock(AiAgentPendingOperationStore.class);
        noteService = mock(NoteService.class);
        service = new AiAgentOperationConfirmService(
                sessionService,
                operationStore,
                noteService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void confirm_validBatchUnpublishOperation_executesNoteService() {
        when(sessionService.requireCurrentEnabledUserId()).thenReturn(7L);
        when(operationStore.consume(7L, "op-1")).thenReturn(Optional.of(operation("op-1")));
        when(noteService.batchUnpublishMyPublishedNotes(List.of(1L, 2L)))
                .thenReturn(new NoteBatchUnpublishResult(2, List.of(note(1L, "first"), note(2L, "second"))));

        AiAgentOperationConfirmResponse response = service.confirm("op-1");

        assertThat(response.operationId()).isEqualTo("op-1");
        assertThat(response.operationType()).isEqualTo("BATCH_UNPUBLISH_NOTES");
        assertThat(response.status()).isEqualTo("EXECUTED");
        assertThat(response.affectedCount()).isEqualTo(2);
        assertThat(response.affectedItems()).hasSize(2);
        verify(noteService).batchUnpublishMyPublishedNotes(List.of(1L, 2L));
    }

    @Test
    void confirm_sameOperationTwice_secondCallFailsWithoutExecutingAgain() {
        when(sessionService.requireCurrentEnabledUserId()).thenReturn(7L);
        when(operationStore.consume(7L, "op-1"))
                .thenReturn(Optional.of(operation("op-1")))
                .thenReturn(Optional.empty());
        when(noteService.batchUnpublishMyPublishedNotes(List.of(1L, 2L)))
                .thenReturn(new NoteBatchUnpublishResult(2, List.of(note(1L, "first"), note(2L, "second"))));

        service.confirm("op-1");

        assertThatThrownBy(() -> service.confirm("op-1"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(noteService).batchUnpublishMyPublishedNotes(List.of(1L, 2L));
    }

    @Test
    void confirm_missingOperation_fails() {
        when(sessionService.requireCurrentEnabledUserId()).thenReturn(7L);
        when(operationStore.consume(7L, "missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm("missing"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verifyNoInteractions(noteService);
    }

    @Test
    void confirm_expiredOperation_fails() {
        when(sessionService.requireCurrentEnabledUserId()).thenReturn(7L);
        when(operationStore.consume(7L, "op-expired"))
                .thenReturn(Optional.of(operation("op-expired", "BATCH_UNPUBLISH_NOTES", "PENDING",
                        NOW.minusSeconds(1))));

        assertThatThrownBy(() -> service.confirm("op-expired"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verifyNoInteractions(noteService);
    }

    @Test
    void confirm_unsupportedOperationType_fails() {
        when(sessionService.requireCurrentEnabledUserId()).thenReturn(7L);
        when(operationStore.consume(7L, "op-other"))
                .thenReturn(Optional.of(operation("op-other", "OTHER", "PENDING", NOW.plusSeconds(60))));

        assertThatThrownBy(() -> service.confirm("op-other"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        verifyNoInteractions(noteService);
    }

    @Test
    void confirm_operationUserMismatch_failsWithoutExecuting() {
        when(sessionService.requireCurrentEnabledUserId()).thenReturn(7L);
        when(operationStore.consume(7L, "op-other-user"))
                .thenReturn(Optional.of(new AiAgentPendingOperation(
                        "op-other-user",
                        "BATCH_UNPUBLISH_NOTES",
                        8L,
                        List.of(1L),
                        NOW,
                        NOW.plusSeconds(60),
                        "PENDING")));

        assertThatThrownBy(() -> service.confirm("op-other-user"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(noteService);
    }

    @Test
    void confirm_disabledUser_failsBeforeConsumingOperation() {
        when(sessionService.requireCurrentEnabledUserId()).thenThrow(new BizException(ErrorCode.USER_DISABLED));

        assertThatThrownBy(() -> service.confirm("op-1"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_DISABLED));
        verify(operationStore, never()).consume(7L, "op-1");
        verifyNoInteractions(noteService);
    }

    private AiAgentPendingOperation operation(String operationId) {
        return operation(operationId, "BATCH_UNPUBLISH_NOTES", "PENDING", NOW.plusSeconds(60));
    }

    private AiAgentPendingOperation operation(String operationId, String operationType, String status, Instant expiresAt) {
        return new AiAgentPendingOperation(
                operationId,
                operationType,
                7L,
                List.of(1L, 2L),
                NOW,
                expiresAt,
                status);
    }

    private NoteListItemResponse note(Long id, String title) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 26, 20, 0);
        return new NoteListItemResponse(id, title, "summary", null, List.of(),
                "PRIVATE", "NORMAL", now, now, now);
    }
}
