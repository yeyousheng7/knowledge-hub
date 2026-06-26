package com.yousheng.knowledgehub.ai.tool.note;

import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.note.dto.NoteToolDetail;
import com.yousheng.knowledgehub.ai.tool.support.AiToolArguments;
import com.yousheng.knowledgehub.ai.tool.support.AiToolResults;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteDetailResponse;
import com.yousheng.knowledgehub.note.service.NoteService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class NoteWriteToolFacade {

    private final NoteService noteService;

    public AiToolResult<NoteToolDetail> publishMyNote(Long noteId) {
        AiToolResult<Long> noteIdResult = AiToolArguments.requireNoteId(noteId);
        if (!noteIdResult.success()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, noteIdResult.message());
        }

        try {
            NoteDetailResponse response = noteService.publishNote(noteIdResult.data());
            List<String> warnings = new ArrayList<>();
            NoteToolDetail detail = NoteReadToolFacade.buildDetail(response, warnings);
            return warnings.isEmpty()
                    ? AiToolResults.success(detail)
                    : AiToolResults.success(detail, warnings);
        } catch (BizException e) {
            return AiToolResults.failure(e);
        }
    }

    public AiToolResult<NoteToolDetail> unpublishMyNote(Long noteId) {
        AiToolResult<Long> noteIdResult = AiToolArguments.requireNoteId(noteId);
        if (!noteIdResult.success()) {
            return AiToolResults.failure(ErrorCode.BAD_REQUEST, noteIdResult.message());
        }

        try {
            NoteDetailResponse response = noteService.unpublishNote(noteIdResult.data());
            List<String> warnings = new ArrayList<>();
            NoteToolDetail detail = NoteReadToolFacade.buildDetail(response, warnings);
            return warnings.isEmpty()
                    ? AiToolResults.success(detail)
                    : AiToolResults.success(detail, warnings);
        } catch (BizException e) {
            return AiToolResults.failure(e);
        }
    }
}
