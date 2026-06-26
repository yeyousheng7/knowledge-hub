package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolDetail;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class NoteWriteTools {

    private final NoteWriteToolFacade facade;

    public NoteWriteTools(NoteWriteToolFacade facade) {
        this.facade = facade;
    }

    @Tool(description = """
            发布当前认证用户的一篇笔记。
            noteId 必须为正数。
            不要传 userId。
            已发布的笔记重复发布是幂等的。""")
    public AiToolResult<NoteToolDetail> publish_my_note(
            @ToolParam(description = "笔记 ID，必须为正数") Long noteId) {
        return facade.publishMyNote(noteId);
    }

    @Tool(description = """
            下架当前认证用户的一篇已发布笔记。
            noteId 必须为正数。
            不要传 userId。
            已下架的笔记重复下架是幂等的。""")
    public AiToolResult<NoteToolDetail> unpublish_my_note(
            @ToolParam(description = "笔记 ID，必须为正数") Long noteId) {
        return facade.unpublishMyNote(noteId);
    }
}
