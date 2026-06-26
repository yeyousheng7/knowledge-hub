package com.yousheng.knowledgehub.ai.tool.note;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NoteActionToolsTest {

    @Test
    void prepareBatchUnpublishPublishedNotes_isReturnDirectTerminalTool() throws Exception {
        NoteActionTools tools = new NoteActionTools(mock(NoteActionToolFacade.class));

        Method method = tools.getClass().getDeclaredMethod("prepare_batch_unpublish_published_notes");
        Tool tool = method.getAnnotation(Tool.class);

        assertThat(tool).isNotNull();
        assertThat(tool.returnDirect()).isTrue();
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
