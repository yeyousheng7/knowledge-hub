package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolPage;
import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolDetail;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolItem;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteDetailResponse;
import com.yousheng.knowledgehub.note.dto.NoteListItemResponse;
import com.yousheng.knowledgehub.note.dto.NoteListResponse;
import com.yousheng.knowledgehub.note.dto.NoteTagResponse;
import com.yousheng.knowledgehub.note.service.NoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteReadToolFacadeTest {

    @Mock
    private NoteService noteService;

    @InjectMocks
    private NoteReadToolFacade facade;

    @Test
    void searchMyNotes_blankKeyword_returnsBadRequest() {
        AiToolResult<AiToolPage<NoteToolItem>> result = facade.searchMyNotes("   ", null, null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void searchMyNotes_nullKeyword_returnsBadRequest() {
        AiToolResult<AiToolPage<NoteToolItem>> result = facade.searchMyNotes(null, null, null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void searchMyNotes_defaultPageAndSize_callsServiceWithDefaults() {
        NoteListResponse response = new NoteListResponse(List.of(), 0, 1, 5);
        when(noteService.listMyNotes(1, 5, null, null, "spring")).thenReturn(response);

        AiToolResult<AiToolPage<NoteToolItem>> result = facade.searchMyNotes("spring", null, null);

        assertThat(result.success()).isTrue();
        assertThat(result.data().page()).isEqualTo(1);
        assertThat(result.data().size()).isEqualTo(5);
        verify(noteService).listMyNotes(1, 5, null, null, "spring");
    }

    @Test
    void searchMyNotes_sizeAbove10_clampsAndWarns() {
        NoteListResponse response = new NoteListResponse(List.of(), 0, 1, 10);
        when(noteService.listMyNotes(1, 10, null, null, "java")).thenReturn(response);

        AiToolResult<AiToolPage<NoteToolItem>> result = facade.searchMyNotes("java", 1, 20);

        assertThat(result.success()).isTrue();
        assertThat(result.data().size()).isEqualTo(10);
        assertThat(result.warnings()).anyMatch(w -> w.contains("10"));
        verify(noteService).listMyNotes(1, 10, null, null, "java");
    }

    @Test
    void searchMyNotes_callsServiceWithTrimmedKeyword() {
        NoteListResponse response = new NoteListResponse(List.of(), 0, 1, 5);
        when(noteService.listMyNotes(1, 5, null, null, "hello")).thenReturn(response);

        facade.searchMyNotes("  hello  ", null, null);

        verify(noteService).listMyNotes(1, 5, null, null, "hello");
    }

    @Test
    void searchMyNotes_hasMoreWhenPageSizeLessThanTotal() {
        NoteTagResponse tag = new NoteTagResponse(1L, "java");
        NoteListItemResponse item = new NoteListItemResponse(
                1L, "title", "summary", null, List.of(tag),
                "PRIVATE", "NORMAL", LocalDateTime.now(), LocalDateTime.now(), null);
        NoteListResponse response = new NoteListResponse(List.of(item), 15, 1, 5);
        when(noteService.listMyNotes(1, 5, null, null, "test")).thenReturn(response);

        AiToolResult<AiToolPage<NoteToolItem>> result = facade.searchMyNotes("test", 1, 5);

        assertThat(result.success()).isTrue();
        assertThat(result.data().hasMore()).isTrue();
        assertThat(result.data().returned()).isEqualTo(1);
    }

    @Test
    void searchMyNotes_hasMoreFalseWhenPageSizeReachesTotal() {
        NoteListResponse response = new NoteListResponse(List.of(), 5, 1, 5);
        when(noteService.listMyNotes(1, 5, null, null, "test")).thenReturn(response);

        AiToolResult<AiToolPage<NoteToolItem>> result = facade.searchMyNotes("test", 1, 5);

        assertThat(result.success()).isTrue();
        assertThat(result.data().hasMore()).isFalse();
    }

    @Test
    void searchMyNotes_itemDoesNotContainContentMd() {
        NoteTagResponse tag = new NoteTagResponse(1L, "java");
        NoteListItemResponse item = new NoteListItemResponse(
                1L, "title", "summary", null, List.of(tag),
                "PRIVATE", "NORMAL", LocalDateTime.now(), LocalDateTime.now(), null);
        NoteListResponse response = new NoteListResponse(List.of(item), 1, 1, 5);
        when(noteService.listMyNotes(1, 5, null, null, "test")).thenReturn(response);

        AiToolResult<AiToolPage<NoteToolItem>> result = facade.searchMyNotes("test", 1, 5);

        NoteToolItem toolItem = result.data().items().get(0);
        assertThat(toolItem.id()).isEqualTo(1L);
        assertThat(toolItem.title()).isEqualTo("title");
        assertThat(toolItem.tags()).hasSize(1);
    }

    @Test
    void searchMyNotes_bizException_convertsToFailure() {
        BizException bizEx = new BizException(ErrorCode.NOTE_NOT_FOUND);
        when(noteService.listMyNotes(anyLong(), anyLong(), isNull(), isNull(), eq("test")))
                .thenThrow(bizEx);

        AiToolResult<AiToolPage<NoteToolItem>> result = facade.searchMyNotes("test", 1, 5);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.NOTE_NOT_FOUND.getCode());
    }

    @Test
    void searchMyNotes_runtimeException_propagates() {
        RuntimeException runtimeEx = new RuntimeException("unexpected");
        when(noteService.listMyNotes(anyLong(), anyLong(), isNull(), isNull(), eq("test")))
                .thenThrow(runtimeEx);

        assertThatThrownBy(() -> facade.searchMyNotes("test", 1, 5))
                .isSameAs(runtimeEx);
    }

    @Test
    void getMyNoteDetail_nullNoteId_returnsBadRequest() {
        AiToolResult<NoteToolDetail> result = facade.getMyNoteDetail(null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void getMyNoteDetail_zeroNoteId_returnsBadRequest() {
        AiToolResult<NoteToolDetail> result = facade.getMyNoteDetail(0L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void getMyNoteDetail_negativeNoteId_returnsBadRequest() {
        AiToolResult<NoteToolDetail> result = facade.getMyNoteDetail(-1L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void getMyNoteDetail_contentWithinLimit_notTruncated() {
        NoteDetailResponse detail = new NoteDetailResponse(
                1L, "title", "short content", "summary", null,
                List.of(), "PRIVATE", "NORMAL",
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(noteService.getMyNoteDetail(1L)).thenReturn(detail);

        AiToolResult<NoteToolDetail> result = facade.getMyNoteDetail(1L);

        assertThat(result.success()).isTrue();
        assertThat(result.data().contentTruncated()).isFalse();
        assertThat(result.data().contentLength()).isEqualTo("short content".length());
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void getMyNoteDetail_contentExceedsLimit_truncatesAndWarns() {
        String longContent = "a".repeat(5000);
        NoteDetailResponse detail = new NoteDetailResponse(
                1L, "title", longContent, "summary", null,
                List.of(), "PRIVATE", "NORMAL",
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(noteService.getMyNoteDetail(1L)).thenReturn(detail);

        AiToolResult<NoteToolDetail> result = facade.getMyNoteDetail(1L);

        assertThat(result.success()).isTrue();
        assertThat(result.data().contentTruncated()).isTrue();
        assertThat(result.data().contentLength()).isEqualTo(5000);
        assertThat(result.data().contentMd()).hasSize(4000);
        assertThat(result.warnings()).anyMatch(w -> w.contains("4000"));
    }

    @Test
    void getMyNoteDetail_contentWithSurrogatePair_truncatesByCodePoint() {
        String original = "a".repeat(3999) + "😀" + "tail";
        NoteDetailResponse detail = new NoteDetailResponse(
                1L, "title", original, "summary", null,
                List.of(), "PRIVATE", "NORMAL",
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(noteService.getMyNoteDetail(1L)).thenReturn(detail);

        AiToolResult<NoteToolDetail> result = facade.getMyNoteDetail(1L);

        assertThat(result.success()).isTrue();
        assertThat(result.data().contentTruncated()).isTrue();
        assertThat(result.data().contentLength()).isEqualTo(4004);
        assertThat(result.data().contentMd().codePointCount(0, result.data().contentMd().length()))
                .isEqualTo(4000);
        assertThat(result.data().contentMd()).doesNotContain("tail");
        assertThat(result.data().contentMd()).endsWith("😀");
        assertThat(result.warnings()).anyMatch(w -> w.contains("4000"));
    }

    @Test
    void getMyNoteDetail_bizException_convertsToFailure() {
        BizException bizEx = new BizException(ErrorCode.NOTE_NOT_FOUND);
        when(noteService.getMyNoteDetail(999L)).thenThrow(bizEx);

        AiToolResult<NoteToolDetail> result = facade.getMyNoteDetail(999L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.NOTE_NOT_FOUND.getCode());
    }

    @Test
    void getMyNoteDetail_runtimeException_propagates() {
        RuntimeException runtimeEx = new RuntimeException("unexpected");
        when(noteService.getMyNoteDetail(1L)).thenThrow(runtimeEx);

        assertThatThrownBy(() -> facade.getMyNoteDetail(1L))
                .isSameAs(runtimeEx);
    }

    @Test
    void listMyPublishedNotes_defaultsPageAndSize() {
        NoteListResponse response = new NoteListResponse(List.of(), 0, 1, 5);
        when(noteService.listMyPublishedNotes(1, 5)).thenReturn(response);

        AiToolResult<AiToolPage<NoteToolItem>> result = facade.listMyPublishedNotes(null, null);

        assertThat(result.success()).isTrue();
        assertThat(result.data().page()).isEqualTo(1);
        assertThat(result.data().size()).isEqualTo(5);
        verify(noteService).listMyPublishedNotes(1, 5);
    }

    @Test
    void listMyPublishedNotes_sizeAbove10_clampsAndWarns() {
        NoteListResponse response = new NoteListResponse(List.of(), 0, 1, 10);
        when(noteService.listMyPublishedNotes(1, 10)).thenReturn(response);

        AiToolResult<AiToolPage<NoteToolItem>> result = facade.listMyPublishedNotes(1, 20);

        assertThat(result.success()).isTrue();
        assertThat(result.data().size()).isEqualTo(10);
        assertThat(result.warnings()).anyMatch(w -> w.contains("10"));
    }

    @Test
    void listMyPublishedNotes_itemDoesNotContainContentMd() {
        NoteTagResponse tag = new NoteTagResponse(1L, "spring");
        NoteListItemResponse item = new NoteListItemResponse(
                1L, "title", "summary", null, List.of(tag),
                "PUBLIC", "NORMAL",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        NoteListResponse response = new NoteListResponse(List.of(item), 1, 1, 5);
        when(noteService.listMyPublishedNotes(1, 5)).thenReturn(response);

        AiToolResult<AiToolPage<NoteToolItem>> result = facade.listMyPublishedNotes(1, 5);

        NoteToolItem toolItem = result.data().items().get(0);
        assertThat(toolItem.id()).isEqualTo(1L);
        assertThat(toolItem.title()).isEqualTo("title");
    }

    @Test
    void listMyPublishedNotes_bizException_convertsToFailure() {
        BizException bizEx = new BizException(ErrorCode.USER_DISABLED);
        when(noteService.listMyPublishedNotes(1, 5)).thenThrow(bizEx);

        AiToolResult<AiToolPage<NoteToolItem>> result = facade.listMyPublishedNotes(1, 5);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.USER_DISABLED.getCode());
    }

    @Test
    void listMyPublishedNotes_runtimeException_propagates() {
        RuntimeException runtimeEx = new RuntimeException("unexpected");
        when(noteService.listMyPublishedNotes(1, 5)).thenThrow(runtimeEx);

        assertThatThrownBy(() -> facade.listMyPublishedNotes(1, 5))
                .isSameAs(runtimeEx);
    }
}
