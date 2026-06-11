package com.yousheng.knowledgehub.admin.controller;

import com.yousheng.knowledgehub.admin.dto.AdminUserListResponse;
import com.yousheng.knowledgehub.admin.dto.AdminUserStatusResponse;
import com.yousheng.knowledgehub.admin.service.AdminUserService;
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
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "获取用户列表")
    @GetMapping
    public ApiResponse<AdminUserListResponse> list(
            @Parameter(description = "页码, 从 1 开始") @RequestParam @Min(1) long page,
            @Parameter(description = "每页数量, 最大 100") @RequestParam @Min(1) @Max(100) long size,
            @Parameter(description = "关键字") @RequestParam(required = false) @Size(max = 100) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(adminUserService.getAppUserList(page, size, keyword, status));
    }


    @Operation(summary = "禁用用户")
    @PostMapping("/{userId}/disable")
    public ApiResponse<AdminUserStatusResponse> disable(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long userId
    ) {
        return ApiResponse.ok(adminUserService.disableAppUser(userId));
    }

    @Operation(summary = "启用用户")
    @PostMapping("/{userId}/enable")
    public ApiResponse<AdminUserStatusResponse> enable(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long userId
    ) {
        return ApiResponse.ok(adminUserService.enableAppUser(userId));
    }
}
