package com.yousheng.knowledgehub.ai.tool.note;

import org.springframework.ai.tool.annotation.Tool;

public class NoteActionTools {

    private final NoteActionToolFacade facade;

    public NoteActionTools(NoteActionToolFacade facade) {
        this.facade = facade;
    }

    @Tool(returnDirect = true, description = """
            为当前认证用户准备批量下架公开笔记的待确认操作。
            仅用于用户明确要求批量下架公开/已发布笔记时。
            不执行真实下架，不调用单篇下架工具。
            不要传 userId、conversationId 或 noteIds。
            工具会重新查询当前用户自己的公开笔记，并返回 PENDING_OPERATION action 给前端确认。""")
    public String prepare_batch_unpublish_published_notes() {
        return facade.prepareBatchUnpublishPublishedNotes();
    }
}
