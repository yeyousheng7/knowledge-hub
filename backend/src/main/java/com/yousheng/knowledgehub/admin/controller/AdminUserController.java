package com.yousheng.knowledgehub.admin.controller;

import com.yousheng.knowledgehub.admin.dto.AdminUserStatusResponse;
import com.yousheng.knowledgehub.admin.service.AdminUserService;
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
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

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
