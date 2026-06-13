package com.yousheng.knowledgehub.user.controller;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.note.dto.PublicNoteListResponse;
import com.yousheng.knowledgehub.user.dto.PublicUserProfileResponse;
import com.yousheng.knowledgehub.user.service.PublicUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @Operation(description = "查询用户公开笔记列表")
    @GetMapping("/{username}/notes")
    public ApiResponse<PublicNoteListResponse> list(
            @Size(min = 3, max = 30)
            @Pattern(regexp = "^[0-9a-zA-Z_]+$")
            @PathVariable String username,
            @Min(1) @RequestParam(defaultValue = "1") long page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.ok(publicUserService.listUserPublicNotes(username, page, size));
    }

}
