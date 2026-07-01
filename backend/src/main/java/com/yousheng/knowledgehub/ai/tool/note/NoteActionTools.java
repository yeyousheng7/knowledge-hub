package com.yousheng.knowledgehub.ai.tool.note;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

public class NoteActionTools {

    private final NoteActionToolFacade facade;

    public NoteActionTools(NoteActionToolFacade facade) {
        this.facade = facade;
    }

    @Tool(returnDirect = true, description = """
            为当前认证用户准备批量下架公开笔记的待确认操作。
            仅用于用户明确要求下架多篇指定的公开/已发布笔记时。
            不执行真实下架，不调用单篇下架工具。
            不要传 userId 或 conversationId。
            noteIds 必填，必须来自当前用户笔记工具的真实结果，最多 20 个。
            工具会校验这些笔记仍属于当前用户且处于可下架状态，并返回 PENDING_OPERATION action 给前端确认。""")
    public String prepare_batch_unpublish_published_notes(
            @ToolParam(description = "要下架的当前用户公开笔记 ID，必填，最多 20 个") List<Long> noteIds) {
        return facade.prepareBatchUnpublishPublishedNotes(noteIds);
    }

    @Tool(returnDirect = true, description = """
            为当前认证用户准备创建私有笔记的待确认操作。
            仅在用户明确要求根据文本、要点或主题创建笔记时使用。
            不要传 userId 或 conversationId。
            title 必填，最大 100 字符。
            contentMd 必填，最大 100000 字符，使用 Markdown。
            summary 可选，最大 300 字符。
            recommendedTags 只是给前端展示的建议，不会自动创建标签。
            工具只生成 PENDING_OPERATION action，不会真实创建笔记，不会发布笔记。""")
    public String prepare_create_private_note(
            @ToolParam(description = "笔记标题，必填，最大 100 字符") String title,
            @ToolParam(description = "Markdown 正文，必填，最大 100000 字符") String contentMd,
            @ToolParam(description = "摘要，可选，最大 300 字符", required = false) String summary,
            @ToolParam(description = "推荐标签名，仅用于展示，不会自动创建标签", required = false)
            List<String> recommendedTags) {
        return facade.prepareCreatePrivateNote(title, contentMd, summary, recommendedTags);
    }
}
