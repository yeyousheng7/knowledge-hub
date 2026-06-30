package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolPage;
import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolDetail;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteReadToolsTest {

    @Mock
    private NoteReadToolFacade facade;

    @InjectMocks
    private NoteReadTools tools;

    @Test
    void search_my_notes_delegatesToFacade() {
        AiToolPage<NoteToolItem> page = new AiToolPage<>(1, 5, 0, false, List.of());
        AiToolResult<AiToolPage<NoteToolItem>> expected = new AiToolResult<>(true, 0, "OK", page, List.of());
        when(facade.searchMyNotes("java", 1, 5)).thenReturn(expected);

        AiToolResult<AiToolPage<NoteToolItem>> result = tools.search_my_notes("java", 1, 5);

        assertThat(result).isSameAs(expected);
        verify(facade).searchMyNotes("java", 1, 5);
    }

    @Test
    void search_my_notes_nullPageAndSize_delegatesToFacade() {
        AiToolPage<NoteToolItem> page = new AiToolPage<>(1, 5, 0, false, List.of());
        AiToolResult<AiToolPage<NoteToolItem>> expected = new AiToolResult<>(true, 0, "OK", page, List.of());
        when(facade.searchMyNotes("test", null, null)).thenReturn(expected);

        AiToolResult<AiToolPage<NoteToolItem>> result = tools.search_my_notes("test", null, null);

        assertThat(result).isSameAs(expected);
        verify(facade).searchMyNotes("test", null, null);
    }

    @Test
    void get_my_note_detail_delegatesToFacade() {
        NoteToolDetail detail = new NoteToolDetail(
                1L, "title", "summary", "content", false, 7,
                List.of(), "PRIVATE", "NORMAL", null, null);
        AiToolResult<NoteToolDetail> expected = new AiToolResult<>(true, 0, "OK", detail, List.of());
        when(facade.getMyNoteDetail(1L)).thenReturn(expected);

        AiToolResult<NoteToolDetail> result = tools.get_my_note_detail(1L);

        assertThat(result).isSameAs(expected);
        verify(facade).getMyNoteDetail(1L);
    }

    @Test
    void list_my_notes_delegatesToFacade() {
        AiToolPage<NoteToolItem> page = new AiToolPage<>(1, 5, 3, false, List.of());
        AiToolResult<AiToolPage<NoteToolItem>> expected = new AiToolResult<>(true, 0, "OK", page, List.of());
        when(facade.listMyNotes(1, 5)).thenReturn(expected);

        AiToolResult<AiToolPage<NoteToolItem>> result = tools.list_my_notes(1, 5);

        assertThat(result).isSameAs(expected);
        verify(facade).listMyNotes(1, 5);
    }

    @Test
    void list_my_notes_nullPageAndSize_delegatesToFacade() {
        AiToolPage<NoteToolItem> page = new AiToolPage<>(1, 5, 0, false, List.of());
        AiToolResult<AiToolPage<NoteToolItem>> expected = new AiToolResult<>(true, 0, "OK", page, List.of());
        when(facade.listMyNotes(null, null)).thenReturn(expected);

        AiToolResult<AiToolPage<NoteToolItem>> result = tools.list_my_notes(null, null);

        assertThat(result).isSameAs(expected);
        verify(facade).listMyNotes(null, null);
    }

    @Test
    void list_my_published_notes_delegatesToFacade() {
        AiToolPage<NoteToolItem> page = new AiToolPage<>(1, 5, 2, false, List.of());
        AiToolResult<AiToolPage<NoteToolItem>> expected = new AiToolResult<>(true, 0, "OK", page, List.of());
        when(facade.listMyPublishedNotes(1, 5)).thenReturn(expected);

        AiToolResult<AiToolPage<NoteToolItem>> result = tools.list_my_published_notes(1, 5);

        assertThat(result).isSameAs(expected);
        verify(facade).listMyPublishedNotes(1, 5);
    }

    @Test
    void list_my_published_notes_nullPageAndSize_delegatesToFacade() {
        AiToolPage<NoteToolItem> page = new AiToolPage<>(1, 5, 0, false, List.of());
        AiToolResult<AiToolPage<NoteToolItem>> expected = new AiToolResult<>(true, 0, "OK", page, List.of());
        when(facade.listMyPublishedNotes(null, null)).thenReturn(expected);

        AiToolResult<AiToolPage<NoteToolItem>> result = tools.list_my_published_notes(null, null);

        assertThat(result).isSameAs(expected);
        verify(facade).listMyPublishedNotes(null, null);
    }
}
