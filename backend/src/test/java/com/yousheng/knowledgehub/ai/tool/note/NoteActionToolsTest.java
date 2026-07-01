package com.yousheng.knowledgehub.ai.tool.note;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteActionToolsTest {

    @Test
    void prepareBatchUnpublishPublishedNotes_isReturnDirectTerminalTool() throws Exception {
        NoteActionToolFacade facade = mock(NoteActionToolFacade.class);
        NoteActionTools tools = new NoteActionTools(facade);
        when(facade.prepareBatchUnpublishPublishedNotes(java.util.List.of(2L, 1L)))
                .thenReturn("pending-action");

        String result = tools.prepare_batch_unpublish_published_notes(java.util.List.of(2L, 1L));

        Method method = tools.getClass().getDeclaredMethod(
                "prepare_batch_unpublish_published_notes",
                java.util.List.class);
        Tool tool = method.getAnnotation(Tool.class);

        assertThat(result).isEqualTo("pending-action");
        assertThat(tool).isNotNull();
        assertThat(tool.returnDirect()).isTrue();
        verify(facade).prepareBatchUnpublishPublishedNotes(java.util.List.of(2L, 1L));
    }

    @Test
    void prepareCreatePrivateNote_isReturnDirectTerminalTool() throws Exception {
        NoteActionTools tools = new NoteActionTools(mock(NoteActionToolFacade.class));

        Method method = tools.getClass().getDeclaredMethod(
                "prepare_create_private_note",
                String.class,
                String.class,
                String.class,
                java.util.List.class);
        Tool tool = method.getAnnotation(Tool.class);

        assertThat(tool).isNotNull();
        assertThat(tool.returnDirect()).isTrue();
    }
}
