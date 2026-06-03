package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.note.dto.PublicNoteDetailResponse;
import com.yousheng.knowledgehub.note.dto.PublicNoteListResponse;
import com.yousheng.knowledgehub.note.service.NoteService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/public/notes")
@Validated
public class PublicNoteController {
    private final NoteService noteService;


    @GetMapping
    public ApiResponse<PublicNoteListResponse> listPublicNotes(
            @Min(1) @RequestParam(defaultValue = "1") long page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.ok(noteService.listPublicNotes(page, size));
    }

    @GetMapping("/{noteId}")
    public ApiResponse<PublicNoteDetailResponse> getPublicNoteDetail(@PathVariable Long noteId) {
        return ApiResponse.ok(noteService.getPublicNoteDetail(noteId));
    }

}
