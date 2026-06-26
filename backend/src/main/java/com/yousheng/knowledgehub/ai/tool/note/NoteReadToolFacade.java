package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolPage;
import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolDetail;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolItem;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolTag;
import com.yousheng.knowledgehub.ai.tool.support.AiToolArguments;
import com.yousheng.knowledgehub.ai.tool.support.AiToolResults;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteDetailResponse;
import com.yousheng.knowledgehub.note.dto.NoteListItemResponse;
import com.yousheng.knowledgehub.note.dto.NoteListResponse;
import com.yousheng.knowledgehub.note.service.NoteService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class NoteReadToolFacade {

    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final String CONTENT_TRUNCATED_WARNING = "正文已截断至 %d 字符。".formatted(MAX_CONTENT_LENGTH);

    private final NoteService noteService;

    public AiToolResult<AiToolPage<NoteToolItem>> searchMyNotes(String keyword, Integer page, Integer size) {
        AiToolResult<String> keywordResult = AiToolArguments.requireKeyword(keyword);
        if (!keywordResult.success()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, keywordResult.message());
        }

        AiToolResult<Integer> pageResult = AiToolArguments.normalizePage(page);
        if (!pageResult.success()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, pageResult.message());
        }

        AiToolResult<Integer> sizeResult = AiToolArguments.normalizeSize(size);
        if (!sizeResult.success()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, sizeResult.message());
        }

        List<String> warnings = new ArrayList<>(sizeResult.warnings());

        int pageValue = pageResult.data();
        int sizeValue = sizeResult.data();
        String trimmedKeyword = keywordResult.data();

        try {
            NoteListResponse response = noteService.listMyNotes(pageValue, sizeValue, null, null, trimmedKeyword);

            List<NoteToolItem> items = response.items().stream()
                    .map(NoteReadToolFacade::toToolItem)
                    .toList();

            boolean hasMore = (long) pageValue * sizeValue < response.total();
            AiToolPage<NoteToolItem> toolPage = new AiToolPage<>(pageValue, sizeValue, items.size(), hasMore, items);

            return warnings.isEmpty()
                    ? AiToolResults.success(toolPage)
                    : AiToolResults.success(toolPage, warnings);
        } catch (BizException e) {
            return AiToolResults.failure(e);
        }
    }

    public AiToolResult<NoteToolDetail> getMyNoteDetail(Long noteId) {
        AiToolResult<Long> noteIdResult = AiToolArguments.requireNoteId(noteId);
        if (!noteIdResult.success()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, noteIdResult.message());
        }

        try {
            NoteDetailResponse response = noteService.getMyNoteDetail(noteIdResult.data());
            List<String> warnings = new ArrayList<>();

            NoteToolDetail detail = buildDetail(response, warnings);

            return warnings.isEmpty()
                    ? AiToolResults.success(detail)
                    : AiToolResults.success(detail, warnings);
        } catch (BizException e) {
            return AiToolResults.failure(e);
        }
    }

    public AiToolResult<AiToolPage<NoteToolItem>> listMyPublishedNotes(Integer page, Integer size) {
        AiToolResult<Integer> pageResult = AiToolArguments.normalizePage(page);
        if (!pageResult.success()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, pageResult.message());
        }

        AiToolResult<Integer> sizeResult = AiToolArguments.normalizeSize(size);
        if (!sizeResult.success()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, sizeResult.message());
        }

        List<String> warnings = new ArrayList<>(sizeResult.warnings());

        int pageValue = pageResult.data();
        int sizeValue = sizeResult.data();

        try {
            NoteListResponse response = noteService.listMyPublishedNotes(pageValue, sizeValue);

            List<NoteToolItem> items = response.items().stream()
                    .map(NoteReadToolFacade::toToolItem)
                    .toList();

            boolean hasMore = (long) pageValue * sizeValue < response.total();
            AiToolPage<NoteToolItem> toolPage = new AiToolPage<>(pageValue, sizeValue, items.size(), hasMore, items);

            return warnings.isEmpty()
                    ? AiToolResults.success(toolPage)
                    : AiToolResults.success(toolPage, warnings);
        } catch (BizException e) {
            return AiToolResults.failure(e);
        }
    }

    private static NoteToolItem toToolItem(NoteListItemResponse item) {
        List<NoteToolTag> tags = item.tags() != null
                ? item.tags().stream()
                .map(t -> new NoteToolTag(t.id(), t.name()))
                .toList()
                : List.of();

        return new NoteToolItem(
                item.id(),
                item.title(),
                item.summary(),
                tags,
                item.visibility(),
                item.moderationStatus(),
                item.publishedAt(),
                item.updatedAt()
        );
    }

    static NoteToolDetail buildDetail(NoteDetailResponse detail, List<String> warnings) {
        List<NoteToolTag> tags = detail.tags() != null
                ? detail.tags().stream()
                .map(t -> new NoteToolTag(t.id(), t.name()))
                .toList()
                : List.of();

        String content = detail.contentMd();
        boolean truncated = false;
        int contentLength = 0;

        if (content != null) {
            contentLength = content.codePointCount(0, content.length());
            if (contentLength > MAX_CONTENT_LENGTH) {
                int safeEndIndex = content.offsetByCodePoints(0, MAX_CONTENT_LENGTH);
                content = content.substring(0, safeEndIndex);
                truncated = true;
                warnings.add(CONTENT_TRUNCATED_WARNING);
            }
        }

        return new NoteToolDetail(
                detail.id(),
                detail.title(),
                detail.summary(),
                content,
                truncated,
                contentLength,
                tags,
                detail.visibility(),
                detail.moderationStatus(),
                detail.publishedAt(),
                detail.updatedAt()
        );
    }
}
