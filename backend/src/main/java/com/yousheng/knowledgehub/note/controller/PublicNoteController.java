package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.note.dto.PublicNoteDetailResponse;
import com.yousheng.knowledgehub.note.dto.PublicNoteListResponse;
import com.yousheng.knowledgehub.note.service.PublicNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Public Note", description = "公开笔记阅读接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/public/notes")
@Validated
public class PublicNoteController {
    private final PublicNoteService publicNoteService;

    @Operation(summary = "查询公开笔记列表")
    @GetMapping
    public ApiResponse<PublicNoteListResponse> listPublicNotes(
            @Parameter(description = "页码，从 1 开始") @Min(1) @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页数量，最大 100") @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size,
            @Parameter(description = "关键字") @Size(max = 100) @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(publicNoteService.listPublicNotes(page, size, keyword));
    }

    @Operation(summary = "查询公开笔记详情")
    @GetMapping("/{noteId}")
    public ApiResponse<PublicNoteDetailResponse> getPublicNoteDetail(
            @Parameter(description = "笔记 ID", required = true) @PathVariable Long noteId) {
        return ApiResponse.ok(publicNoteService.getPublicNoteDetail(noteId));
    }
}
