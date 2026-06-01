package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.note.dto.NoteCreateRequest;
import com.yousheng.knowledgehub.note.dto.NoteCreateResponse;
import com.yousheng.knowledgehub.note.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {
    private final NoteService noteService;

    @PostMapping
    public ApiResponse<NoteCreateResponse> create(@Valid @RequestBody NoteCreateRequest noteCreateRequest) {
        return ApiResponse.ok(noteService.createNote(noteCreateRequest));
    }
}
