package com.yousheng.knowledgehub.tag.controller;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.config.openapi.OpenApiConfig;
import com.yousheng.knowledgehub.tag.dto.*;
import com.yousheng.knowledgehub.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tag", description = "Tag 管理接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {
    private final TagService tagService;

    @Operation(summary = "获取我的标签列表")
    @GetMapping
    public ApiResponse<TagListResponse> list() {
        return ApiResponse.ok(tagService.listMyTags());
    }

    @Operation(summary = "创建标签")
    @PostMapping
    public ApiResponse<TagCreateResponse> create(@Valid @RequestBody TagCreateRequest tagCreateRequest) {
        return ApiResponse.ok(tagService.createTag(tagCreateRequest));
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{tagId}")
    public ApiResponse<TagUpdateResponse> update(@PathVariable Long tagId, @Valid @RequestBody TagUpdateRequest tagUpdateRequest) {
        return ApiResponse.ok(tagService.updateTag(tagId, tagUpdateRequest));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{tagId}")
    public ApiResponse<Void> delete(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return ApiResponse.ok();
    }


}
