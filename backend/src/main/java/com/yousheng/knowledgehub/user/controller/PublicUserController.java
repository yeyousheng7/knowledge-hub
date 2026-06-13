package com.yousheng.knowledgehub.user.controller;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.user.dto.PublicUserProfileResponse;
import com.yousheng.knowledgehub.user.service.PublicUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "user", description = "公开用户接口")
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/v1/public/users")
public class PublicUserController {
    private final PublicUserService publicUserService;

    @Operation(description = "获取公开用户主页信息")
    @GetMapping("/{username}")
    public ApiResponse<PublicUserProfileResponse> profile(
            @Size(min = 3, max = 30)
            @Pattern(regexp = "^[0-9a-zA-Z_]+$")
            @PathVariable String username
    ) {
        return ApiResponse.ok(publicUserService.getPublicUserProfile(username));
    }

}
