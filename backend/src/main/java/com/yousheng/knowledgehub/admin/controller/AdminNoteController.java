package com.yousheng.knowledgehub.admin.controller;

import com.yousheng.knowledgehub.admin.dto.AdminNoteDetailResponse;
import com.yousheng.knowledgehub.admin.dto.AdminNoteListResponse;
import com.yousheng.knowledgehub.admin.dto.AdminNoteModerationResponse;
import com.yousheng.knowledgehub.admin.service.AdminNoteService;
import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.config.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "admin", description = "管理员接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/notes")
public class AdminNoteController {
    private final AdminNoteService adminNoteService;

    @Operation(summary = "获取笔记审核列表")
    @GetMapping
    public ApiResponse<AdminNoteListResponse> list(
            @Parameter(description = "页码，最小为 1") @Min(1) @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "页码大小，最大为 100") @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size,
            @Parameter(description = "关键字") @Size(max = 100) @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) String moderationStatus
    ) {
        return ApiResponse.ok(adminNoteService.listPublicNotesForAdmin(page, size, keyword, moderationStatus));
    }

    @Operation(summary = "获取笔记详情")
    @GetMapping("/{noteId}")
    public ApiResponse<AdminNoteDetailResponse> detail(@PathVariable Long noteId) {
        return ApiResponse.ok(adminNoteService.getNoteDetail(noteId));
    }

    @Operation(summary = "下架公开笔记")
    @PostMapping("/{noteId}/take-down")
    public ApiResponse<AdminNoteModerationResponse> takeDown(
            @Parameter(description = "笔记 ID", required = true) @PathVariable Long noteId
    ) {
        return ApiResponse.ok(adminNoteService.takeDownNote(noteId));
    }

    @Operation(summary = "恢复公开笔记")
    @PostMapping("/{noteId}/restore")
    public ApiResponse<AdminNoteModerationResponse> restore(
            @Parameter(description = "笔记 ID", required = true) @PathVariable Long noteId
    ) {
        return ApiResponse.ok(adminNoteService.restoreNote(noteId));
    }
}
