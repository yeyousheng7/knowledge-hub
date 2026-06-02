package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.note.dto.*;
import com.yousheng.knowledgehub.note.service.NoteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {
    private final NoteService noteService;

    @PostMapping
    public ApiResponse<NoteCreateResponse> create(@Valid @RequestBody NoteCreateRequest noteCreateRequest) {
        return ApiResponse.ok(noteService.createNote(noteCreateRequest));
    }

    @GetMapping("/{noteId}")
    public ApiResponse<NoteDetailResponse> detail(@PathVariable Long noteId) {
        return ApiResponse.ok(noteService.getMyNoteDetail(noteId));
    }

    @GetMapping
    public ApiResponse<NoteListResponse> list(
            @Min(1) @RequestParam(defaultValue = "1") long page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.ok(noteService.listMyNotes(page, size));
    }

    @PutMapping("/{noteId}")
    public ApiResponse<NoteDetailResponse> update(
            @PathVariable Long noteId,
            @Valid @RequestBody NoteUpdateRequest noteUpdateRequest
    ) {
        return ApiResponse.ok(noteService.updateNote(noteId, noteUpdateRequest));
    }
}
