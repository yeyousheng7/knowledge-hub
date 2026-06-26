package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolPage;
import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.PublicNoteToolItem;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.PublicNoteAuthorResponse;
import com.yousheng.knowledgehub.note.dto.PublicNoteListItemResponse;
import com.yousheng.knowledgehub.note.dto.PublicNoteListResponse;
import com.yousheng.knowledgehub.note.dto.PublicNoteTagResponse;
import com.yousheng.knowledgehub.note.service.PublicNoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicNoteToolFacadeTest {

    @Mock
    private PublicNoteService publicNoteService;

    @InjectMocks
    private PublicNoteToolFacade facade;

    @Test
    void searchPublicNotes_blankKeyword_returnsBadRequest() {
        AiToolResult<AiToolPage<PublicNoteToolItem>> result = facade.searchPublicNotes("   ", null, null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void searchPublicNotes_success_returnsPublicNotes() {
        PublicNoteTagResponse tag = new PublicNoteTagResponse("spring");
        PublicNoteAuthorResponse author = new PublicNoteAuthorResponse("alice", "Alice");
        PublicNoteListItemResponse item = new PublicNoteListItemResponse(
                1L, "Spring Boot Guide", "A comprehensive guide", List.of(tag), author,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 6, 1, 0, 0));
        PublicNoteListResponse response = new PublicNoteListResponse(List.of(item), 1, 1, 5);
        when(publicNoteService.listPublicNotes(1, 5, "spring")).thenReturn(response);

        AiToolResult<AiToolPage<PublicNoteToolItem>> result = facade.searchPublicNotes("spring", null, null);

        assertThat(result.success()).isTrue();
        assertThat(result.data().items()).hasSize(1);
        PublicNoteToolItem toolItem = result.data().items().get(0);
        assertThat(toolItem.id()).isEqualTo(1L);
        assertThat(toolItem.title()).isEqualTo("Spring Boot Guide");
        assertThat(toolItem.summary()).isEqualTo("A comprehensive guide");
        assertThat(toolItem.tags()).hasSize(1);
        assertThat(toolItem.tags().get(0).name()).isEqualTo("spring");
        assertThat(toolItem.author()).isNotNull();
        assertThat(toolItem.author().username()).isEqualTo("alice");
        assertThat(toolItem.author().nickname()).isEqualTo("Alice");
        assertThat(toolItem.publishedAt()).isNotNull();
        assertThat(toolItem.updatedAt()).isNotNull();
        verify(publicNoteService).listPublicNotes(1, 5, "spring");
    }

    @Test
    void searchPublicNotes_emptyResult_returnsSuccessWithEmptyItems() {
        PublicNoteListResponse response = new PublicNoteListResponse(List.of(), 0, 1, 5);
        when(publicNoteService.listPublicNotes(1, 5, "nonexistent")).thenReturn(response);

        AiToolResult<AiToolPage<PublicNoteToolItem>> result = facade.searchPublicNotes("nonexistent", 1, 5);

        assertThat(result.success()).isTrue();
        assertThat(result.data().items()).isEmpty();
        assertThat(result.data().hasMore()).isFalse();
    }

    @Test
    void searchPublicNotes_bizException_convertsToFailure() {
        BizException bizEx = new BizException(ErrorCode.NOTE_NOT_FOUND);
        when(publicNoteService.listPublicNotes(1, 5, "test")).thenThrow(bizEx);

        AiToolResult<AiToolPage<PublicNoteToolItem>> result = facade.searchPublicNotes("test", 1, 5);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.NOTE_NOT_FOUND.getCode());
    }
}
