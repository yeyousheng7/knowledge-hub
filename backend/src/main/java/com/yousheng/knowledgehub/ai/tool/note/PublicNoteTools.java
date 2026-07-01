package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolPage;
import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.PublicNoteToolDetail;
import com.yousheng.knowledgehub.ai.tool.note.dto.PublicNoteToolItem;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class PublicNoteTools {

    private final PublicNoteToolFacade facade;

    public PublicNoteTools(PublicNoteToolFacade facade) {
        this.facade = facade;
    }

    @Tool(description = """
            搜索系统中的公开笔记。
            用于用户想查找别人公开的笔记、全站公开内容。
            不用于搜索当前用户自己的私有笔记；私有笔记请使用 search_my_notes。
            keyword 必填，1-100 字符。
            page 从 1 开始，默认 1。
            size 默认 5，最大 10。
            列表结果不包含 contentMd。
            不要用它来无关键词列出全部公开笔记。""")
    public AiToolResult<AiToolPage<PublicNoteToolItem>> search_public_notes(
            @ToolParam(description = "搜索关键词，必填，1-100 字符") String keyword,
            @ToolParam(description = "页码，从 1 开始，默认 1", required = false) Integer page,
            @ToolParam(description = "每页条数，默认 5，最大 10", required = false) Integer size) {
        return facade.searchPublicNotes(keyword, page, size);
    }

    @Tool(description = """
            分页列出系统中的公开笔记，按发布时间从新到旧排序。
            用于用户想浏览公开内容、查看最近公开笔记且没有明确搜索关键词的场景。
            有明确关键词时请使用 search_public_notes。
            page 从 1 开始，默认 1。
            size 默认 5，最大 10。
            列表结果不包含 contentMd。""")
    public AiToolResult<AiToolPage<PublicNoteToolItem>> list_public_notes(
            @ToolParam(description = "页码，从 1 开始，默认 1", required = false) Integer page,
            @ToolParam(description = "每页条数，默认 5，最大 10", required = false) Integer size) {
        return facade.listPublicNotes(page, size);
    }

    @Tool(description = """
            获取一篇系统公开笔记的详情。
            只在已知具体 noteId 时使用，通常来自 search_public_notes 结果。
            noteId 必须为正数。
            只能获取公开可访问笔记。
            不用于获取当前用户私有笔记；私有笔记请使用 get_my_note_detail。
            contentMd 可能被截断，检查 contentTruncated 和 warnings。""")
    public AiToolResult<PublicNoteToolDetail> get_public_note_detail(
            @ToolParam(description = "笔记 ID，必须为正数") Long noteId) {
        return facade.getPublicNoteDetail(noteId);
    }
}
