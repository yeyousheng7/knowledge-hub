package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.note.dto.NoteCreateRequest;
import com.yousheng.knowledgehub.note.dto.NoteCreateResponse;
import com.yousheng.knowledgehub.note.dto.NoteDetailResponse;
import com.yousheng.knowledgehub.note.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
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
}
