package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.config.openapi.OpenApiConfig;
import com.yousheng.knowledgehub.note.dto.*;
import com.yousheng.knowledgehub.note.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Note", description = "私有笔记管理接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {
    private final NoteService noteService;

    @Operation(summary = "创建私有笔记")
    @PostMapping
    public ApiResponse<NoteCreateResponse> create(@Valid @RequestBody NoteCreateRequest noteCreateRequest) {
        return ApiResponse.ok(noteService.createNote(noteCreateRequest));
    }

    @Operation(summary = "查询我的笔记详情")
    @GetMapping("/{noteId}")
    public ApiResponse<NoteDetailResponse> detail(
            @Parameter(description = "笔记 ID", required = true) @PathVariable Long noteId) {
        return ApiResponse.ok(noteService.getMyNoteDetail(noteId));
    }

    @Operation(summary = "查询我的笔记列表")
    @GetMapping
    public ApiResponse<NoteListResponse> list(
            @Parameter(description = "页码，从 1 开始") @Min(1) @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页数量，最大 100") @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size,
            @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId
    ) {
        return ApiResponse.ok(noteService.listMyNotes(page, size, categoryId));
    }

    @Operation(summary = "更新我的笔记")
    @PutMapping("/{noteId}")
    public ApiResponse<NoteDetailResponse> update(
            @Parameter(description = "笔记 ID", required = true) @PathVariable Long noteId,
            @Valid @RequestBody NoteUpdateRequest noteUpdateRequest
    ) {
        return ApiResponse.ok(noteService.updateNote(noteId, noteUpdateRequest));
    }

    @Operation(summary = "软删除我的笔记")
    @DeleteMapping("/{noteId}")
    public ApiResponse<Void> delete(
            @Parameter(description = "笔记 ID", required = true) @PathVariable Long noteId) {
        noteService.deleteNote(noteId);
        return ApiResponse.ok();
    }

    @Operation(summary = "发布笔记")
    @PostMapping("/{noteId}/publish")
    public ApiResponse<NoteDetailResponse> publish(
            @Parameter(description = "笔记 ID", required = true) @PathVariable Long noteId) {
        return ApiResponse.ok(noteService.publishNote(noteId));
    }

    @Operation(summary = "取消发布笔记")
    @PostMapping("/{noteId}/unpublish")
    public ApiResponse<NoteDetailResponse> unpublish(
            @Parameter(description = "笔记 ID", required = true) @PathVariable Long noteId) {
        return ApiResponse.ok(noteService.unpublishNote(noteId));
    }
}
