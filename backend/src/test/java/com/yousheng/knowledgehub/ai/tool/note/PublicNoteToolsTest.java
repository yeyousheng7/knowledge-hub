package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolPage;
import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.PublicNoteToolItem;
import com.yousheng.knowledgehub.ai.tool.support.AiToolResults;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicNoteToolsTest {

    @Test
    void listPublicNotesDelegatesToFacade() {
        PublicNoteToolFacade facade = mock(PublicNoteToolFacade.class);
        PublicNoteTools tools = new PublicNoteTools(facade);
        AiToolResult<AiToolPage<PublicNoteToolItem>> expected = AiToolResults.success(
                new AiToolPage<>(1, 5, 0, false, List.of()));
        when(facade.listPublicNotes(1, 5)).thenReturn(expected);

        AiToolResult<AiToolPage<PublicNoteToolItem>> result = tools.list_public_notes(1, 5);

        assertThat(result).isSameAs(expected);
        verify(facade).listPublicNotes(1, 5);
    }
}
