package com.yousheng.knowledgehub.user.controller;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.config.openapi.OpenApiConfig;
import com.yousheng.knowledgehub.user.dto.UserPasswordUpdateRequest;
import com.yousheng.knowledgehub.user.dto.UserProfileResponse;
import com.yousheng.knowledgehub.user.dto.UserProfileUpdateRequest;
import com.yousheng.knowledgehub.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "user", description = "用户信息接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> profile() {
        return ApiResponse.ok(userProfileService.getMyProfile());
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.ok(userProfileService.updateMyProfile(request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@Valid @RequestBody UserPasswordUpdateRequest request) {
        userProfileService.updateCurrentUserPassword(request);
        return ApiResponse.ok();
    }

}
