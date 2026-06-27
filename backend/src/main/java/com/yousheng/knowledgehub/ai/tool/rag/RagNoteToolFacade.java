package com.yousheng.knowledgehub.ai.tool.rag;

import com.yousheng.knowledgehub.ai.config.AiProperties;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchHit;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchResult;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchService;
import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.rag.dto.RagNoteToolHit;
import com.yousheng.knowledgehub.ai.tool.rag.dto.RagNoteToolSearchResult;
import com.yousheng.knowledgehub.ai.tool.support.AiToolResults;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;

public class RagNoteToolFacade {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 10;
    private static final String QUERY_MUST_NOT_BE_BLANK = "query 不能为空。";
    private static final String QUERY_TOO_LONG = "query 不能超过 1000 个字符。";
    private static final String TOP_K_CLAMPED_WARNING = "topK 超过上限，已调整为 %d。".formatted(MAX_TOP_K);
    private static final String RAG_UNAVAILABLE_MESSAGE = "RAG 检索当前不可用。";
    private static final int MAX_QUERY_LENGTH = 1000;

    private final AiIndexSearchService searchService;
    private final AiProperties aiProperties;

    public RagNoteToolFacade(ObjectProvider<AiIndexSearchService> searchServiceProvider, AiProperties aiProperties) {
        this.searchService = searchServiceProvider.getIfAvailable();
        this.aiProperties = aiProperties;
    }

    public AiToolResult<RagNoteToolSearchResult> searchMyNotes(String query, Integer topK) {
        if (searchService == null) {
            return AiToolResults.failure(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE, RAG_UNAVAILABLE_MESSAGE);
        }

        AiToolResult<String> queryResult = requireQuery(query);
        if (!queryResult.success()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, queryResult.message());
        }

        AiToolResult<Integer> topKResult = normalizeTopK(topK);
        List<String> warnings = new ArrayList<>(topKResult.warnings());

        String trimmedQuery = queryResult.data();
        int topKValue = topKResult.data();

        try {
            AiIndexSearchResult result = searchService.search(trimmedQuery, topKValue);

            List<RagNoteToolHit> hits = result.hits().stream()
                    .map(RagNoteToolFacade::toToolHit)
                    .toList();

            RagNoteToolSearchResult searchResult = new RagNoteToolSearchResult(trimmedQuery, topKValue, hits);

            return warnings.isEmpty()
                    ? AiToolResults.success(searchResult)
                    : AiToolResults.success(searchResult, warnings);
        } catch (BizException e) {
            return AiToolResults.failure(e);
        }
    }

    private static AiToolResult<String> requireQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, QUERY_MUST_NOT_BE_BLANK);
        }
        String trimmed = query.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, QUERY_TOO_LONG);
        }
        return AiToolResults.success(trimmed);
    }

    private AiToolResult<Integer> normalizeTopK(Integer topK) {
        int candidate;
        if (topK != null) {
            candidate = topK;
        } else {
            int configTopK = aiProperties.getIndex().getTopK();
            candidate = configTopK > 0 ? configTopK : DEFAULT_TOP_K;
        }

        if (candidate <= 0) {
            return AiToolResults.success(DEFAULT_TOP_K);
        }
        if (candidate > MAX_TOP_K) {
            return AiToolResults.success(MAX_TOP_K, List.of(TOP_K_CLAMPED_WARNING));
        }
        return AiToolResults.success(candidate);
    }

    private static RagNoteToolHit toToolHit(AiIndexSearchHit hit) {
        return new RagNoteToolHit(
                hit.noteId(),
                hit.title(),
                hit.chunkText(),
                hit.chunkIndex(),
                hit.distance()
        );
    }
}
