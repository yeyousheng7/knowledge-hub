package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolPage;
import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolAuthor;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolTag;
import com.yousheng.knowledgehub.ai.tool.note.dto.PublicNoteToolItem;
import com.yousheng.knowledgehub.ai.tool.support.AiToolArguments;
import com.yousheng.knowledgehub.ai.tool.support.AiToolResults;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.PublicNoteListResponse;
import com.yousheng.knowledgehub.note.service.PublicNoteService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PublicNoteToolFacade {

    private final PublicNoteService publicNoteService;

    public AiToolResult<AiToolPage<PublicNoteToolItem>> searchPublicNotes(String keyword, Integer page, Integer size) {
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
            PublicNoteListResponse response = publicNoteService.listPublicNotes(pageValue, sizeValue, trimmedKeyword);

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
}
