package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolPage;
import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolDetail;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolItem;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class NoteReadTools {

    private final NoteReadToolFacade facade;

    public NoteReadTools(NoteReadToolFacade facade) {
        this.facade = facade;
    }

    @Tool(description = """
            搜索当前认证用户自己的笔记。
            用于用户想按关键词查找自己的笔记。
            keyword 必填，1-100 字符。
            不要传 userId。
            page 从 1 开始，默认 1。
            size 默认 5，最大 10。
            列表结果不包含 contentMd。
            不要用它来无关键词列出全部笔记。""")
    public AiToolResult<AiToolPage<NoteToolItem>> search_my_notes(
            @ToolParam(description = "搜索关键词，必填，1-100 字符") String keyword,
            @ToolParam(description = "页码，从 1 开始，默认 1", required = false) Integer page,
            @ToolParam(description = "每页条数，默认 5，最大 10", required = false) Integer size) {
        return facade.searchMyNotes(keyword, page, size);
    }

    @Tool(description = """
            获取当前认证用户拥有的一篇笔记详情。
            只在已知具体 noteId 时使用，通常来自 search/list 结果。
            不要传 userId。
            noteId 必须为正数。
            contentMd 可能被截断，检查 contentTruncated 和 warnings。""")
    public AiToolResult<NoteToolDetail> get_my_note_detail(
            @ToolParam(description = "笔记 ID，必须为正数") Long noteId) {
        return facade.getMyNoteDetail(noteId);
    }

    @Tool(description = """
            列出当前认证用户自己的已发布笔记。
            用于用户询问"我有哪些公开/已发布笔记"。
            不用于查询其他用户公开笔记。
            不要传 userId。
            page 默认 1，size 默认 5，最大 10。
            列表结果不包含 contentMd。""")
    public AiToolResult<AiToolPage<NoteToolItem>> list_my_published_notes(
            @ToolParam(description = "页码，从 1 开始，默认 1", required = false) Integer page,
            @ToolParam(description = "每页条数，默认 5，最大 10", required = false) Integer size) {
        return facade.listMyPublishedNotes(page, size);
    }
}
