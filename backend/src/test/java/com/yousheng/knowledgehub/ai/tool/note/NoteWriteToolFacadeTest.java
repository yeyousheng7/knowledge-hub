package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolDetail;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteDetailResponse;
import com.yousheng.knowledgehub.note.service.NoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteWriteToolFacadeTest {

    @Mock
    private NoteService noteService;

    @InjectMocks
    private NoteWriteToolFacade facade;

    @Test
    void publishMyNote_success() {
        NoteDetailResponse response = new NoteDetailResponse(
                1L, "title", "content", "summary", null,
                List.of(), "PUBLIC", "NORMAL",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(noteService.publishNote(1L)).thenReturn(response);

        AiToolResult<NoteToolDetail> result = facade.publishMyNote(1L);

        assertThat(result.success()).isTrue();
        assertThat(result.data().id()).isEqualTo(1L);
        assertThat(result.data().visibility()).isEqualTo("PUBLIC");
        verify(noteService).publishNote(1L);
    }

    @Test
    void unpublishMyNote_success() {
        NoteDetailResponse response = new NoteDetailResponse(
                1L, "title", "content", "summary", null,
                List.of(), "PRIVATE", "NORMAL",
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(noteService.unpublishNote(1L)).thenReturn(response);

        AiToolResult<NoteToolDetail> result = facade.unpublishMyNote(1L);

        assertThat(result.success()).isTrue();
        assertThat(result.data().id()).isEqualTo(1L);
        assertThat(result.data().visibility()).isEqualTo("PRIVATE");
        verify(noteService).unpublishNote(1L);
    }

    @Test
    void publishMyNote_invalidNoteId_noServiceCall() {
        AiToolResult<NoteToolDetail> result = facade.publishMyNote(null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        verifyNoInteractions(noteService);
    }

    @Test
    void unpublishMyNote_invalidNoteId_noServiceCall() {
        AiToolResult<NoteToolDetail> result = facade.unpublishMyNote(0L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        verifyNoInteractions(noteService);
    }

    @Test
    void publishMyNote_bizException_convertsToFailure() {
        BizException bizEx = new BizException(ErrorCode.NOTE_NOT_FOUND);
        when(noteService.publishNote(999L)).thenThrow(bizEx);

        AiToolResult<NoteToolDetail> result = facade.publishMyNote(999L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.NOTE_NOT_FOUND.getCode());
    }

    @Test
    void unpublishMyNote_bizException_convertsToFailure() {
        BizException bizEx = new BizException(ErrorCode.USER_DISABLED);
        when(noteService.unpublishNote(1L)).thenThrow(bizEx);

        AiToolResult<NoteToolDetail> result = facade.unpublishMyNote(1L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.USER_DISABLED.getCode());
    }
}
