package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolPage;
import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolAuthor;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolTag;
import com.yousheng.knowledgehub.ai.tool.note.dto.PublicNoteToolDetail;
import com.yousheng.knowledgehub.ai.tool.note.dto.PublicNoteToolItem;
import com.yousheng.knowledgehub.ai.tool.support.AiToolArguments;
import com.yousheng.knowledgehub.ai.tool.support.AiToolResults;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.PublicNoteDetailResponse;
import com.yousheng.knowledgehub.note.dto.PublicNoteListResponse;
import com.yousheng.knowledgehub.note.service.PublicNoteService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PublicNoteToolFacade {

    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final String CONTENT_TRUNCATED_WARNING = "正文已截断至 %d 字符。".formatted(MAX_CONTENT_LENGTH);

    private final PublicNoteService publicNoteService;

    public AiToolResult<AiToolPage<PublicNoteToolItem>> searchPublicNotes(String keyword, Integer page, Integer size) {
        AiToolResult<String> keywordResult = AiToolArguments.requireKeyword(keyword);
        if (!keywordResult.success()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, keywordResult.message());
        }

        return fetchPublicNotes(keywordResult.data(), page, size);
    }

    public AiToolResult<AiToolPage<PublicNoteToolItem>> listPublicNotes(Integer page, Integer size) {
        return fetchPublicNotes(null, page, size);
    }

    private AiToolResult<AiToolPage<PublicNoteToolItem>> fetchPublicNotes(
            String keyword,
            Integer page,
            Integer size) {

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
            PublicNoteListResponse response = publicNoteService.listPublicNotes(pageValue, sizeValue, keyword);

            List<PublicNoteToolItem> items = response.items().stream()
                    .map(item -> {
                        List<NoteToolTag> tags = item.tags() != null
                                ? item.tags().stream()
                                .map(t -> new NoteToolTag(null, t.name()))
                                .toList()
                                : List.of();

                        NoteToolAuthor author = item.author() != null
                                ? new NoteToolAuthor(item.author().username(), item.author().nickname())
                                : null;

                        return new PublicNoteToolItem(
                                item.id(),
                                item.title(),
                                item.summary(),
                                tags,
                                author,
                                item.publishedAt(),
                                item.updatedAt()
                        );
                    })
                    .toList();

            boolean hasMore = (long) pageValue * sizeValue < response.total();
            AiToolPage<PublicNoteToolItem> toolPage = new AiToolPage<>(pageValue, sizeValue, items.size(), hasMore, items);

            return warnings.isEmpty()
                    ? AiToolResults.success(toolPage)
                    : AiToolResults.success(toolPage, warnings);
        } catch (BizException e) {
            return AiToolResults.failure(e);
        }
    }

    public AiToolResult<PublicNoteToolDetail> getPublicNoteDetail(Long noteId) {
        AiToolResult<Long> noteIdResult = AiToolArguments.requireNoteId(noteId);
        if (!noteIdResult.success()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, noteIdResult.message());
        }

        try {
            PublicNoteDetailResponse response = publicNoteService.getPublicNoteDetail(noteIdResult.data());
            List<String> warnings = new ArrayList<>();

            List<NoteToolTag> tags = response.tags() != null
                    ? response.tags().stream()
                    .map(t -> new NoteToolTag(null, t.name()))
                    .toList()
                    : List.of();

            NoteToolAuthor author = response.author() != null
                    ? new NoteToolAuthor(response.author().username(), response.author().nickname())
                    : null;

            String content = response.contentMd();
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

            PublicNoteToolDetail detail = new PublicNoteToolDetail(
                    response.id(),
                    response.title(),
                    response.summary(),
                    content,
                    truncated,
                    contentLength,
                    tags,
                    author,
                    response.publishedAt(),
                    response.updatedAt()
            );

            return warnings.isEmpty()
                    ? AiToolResults.success(detail)
                    : AiToolResults.success(detail, warnings);
        } catch (BizException e) {
            return AiToolResults.failure(e);
        }
    }
}
