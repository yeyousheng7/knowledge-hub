package com.yousheng.knowledgehub.admin.controller;

import com.yousheng.knowledgehub.admin.dto.AdminNoteModerationResponse;
import com.yousheng.knowledgehub.admin.service.AdminNoteService;
import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.config.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin", description = "管理员接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/notes")
public class AdminNoteController {
    private final AdminNoteService adminNoteService;

    @Operation(summary = "下架公开笔记")
    @PostMapping("/{noteId}/take-down")
    public ApiResponse<AdminNoteModerationResponse> takeDown(
            @Parameter(description = "笔记 ID", required = true) @PathVariable Long noteId
    ) {
        return ApiResponse.ok(adminNoteService.takeDownNote(noteId));
    }
}
