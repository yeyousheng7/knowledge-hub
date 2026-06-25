package com.yousheng.knowledgehub.ai.tool.support;

import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.common.exception.ErrorCode;

import java.util.List;

public final class AiToolArguments {

    private static final String PAGE_MUST_BE_POSITIVE = "page 必须大于 0。";
    private static final String SIZE_MUST_BE_POSITIVE = "size 必须大于 0。";
    private static final String KEYWORD_MUST_NOT_BE_BLANK = "keyword 不能为空。";
    private static final String KEYWORD_TOO_LONG = "keyword 不能超过 100 个字符。";
    private static final String NOTE_ID_MUST_BE_POSITIVE = "noteId 必须大于 0。";
    private static final String SIZE_CLAMPED_WARNING = "size 超过上限，已调整为 10。";
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_PAGE_SIZE = 10;

    private AiToolArguments() {
    }

    public static AiToolResult<Integer> normalizePage(Integer page) {
        if (page == null) {
            return AiToolResults.success(1);
        }
        if (page <= 0) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, PAGE_MUST_BE_POSITIVE);
        }
        return AiToolResults.success(page);
    }

    public static AiToolResult<Integer> normalizeSize(Integer size) {
        if (size == null) {
            return AiToolResults.success(5);
        }
        if (size <= 0) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, SIZE_MUST_BE_POSITIVE);
        }
        if (size > MAX_PAGE_SIZE) {
            return AiToolResults.success(MAX_PAGE_SIZE, List.of(SIZE_CLAMPED_WARNING));
        }
        return AiToolResults.success(size);
    }

    public static AiToolResult<String> requireKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, KEYWORD_MUST_NOT_BE_BLANK);
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, KEYWORD_TOO_LONG);
        }
        return AiToolResults.success(trimmed);
    }

    public static AiToolResult<Long> requireNoteId(Long noteId) {
        if (noteId == null || noteId <= 0) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, NOTE_ID_MUST_BE_POSITIVE);
        }
        return AiToolResults.success(noteId);
    }
}
