package com.yousheng.knowledgehub.ai.tool.rag;

import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.rag.dto.RagNoteToolSearchResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class RagNoteTools {

    private final RagNoteToolFacade facade;

    public RagNoteTools(RagNoteToolFacade facade) {
        this.facade = facade;
    }

    @Tool(description = """
            对当前用户自己的笔记进行语义检索。
            适合用户问题不一定包含准确关键词时使用。
            只检索当前登录用户自己的笔记索引。
            不用于搜索公开笔记；公开笔记请使用 search_public_notes。
            不用于获取完整笔记详情；完整详情请使用 get_my_note_detail。
            query 必填，1-1000 字符。
            topK 可选，默认使用检索配置, 最大 10。
            返回语义匹配的笔记片段，由 Agent 自行生成最终回答。
            如果工具返回 success=false 且 code 为 50302，说明 RAG 检索不可用，应向用户解释。""")
    public AiToolResult<RagNoteToolSearchResult> rag_search_my_notes(
            @ToolParam(description = "语义检索查询，必填，1-1000 字符") String query,
            @ToolParam(description = "返回结果数，默认使用检索配置，最大 10", required = false) Integer topK) {
        return facade.searchMyNotes(query, topK);
    }
}
