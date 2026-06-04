package com.yousheng.knowledgehub.category.controller;

import com.yousheng.knowledgehub.category.dto.CategoryCreateRequest;
import com.yousheng.knowledgehub.category.dto.CategoryCreateResponse;
import com.yousheng.knowledgehub.category.dto.CategoryListResponse;
import com.yousheng.knowledgehub.category.service.CategoryService;
import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.config.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Category", description = "笔记分类管理接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(summary = "获取我的笔记分类")
    @GetMapping
    public ApiResponse<CategoryListResponse> list() {
        return ApiResponse.ok(categoryService.listMyCategories());
    }

    @Operation(summary = "创建笔记分类")
    @PostMapping
    public ApiResponse<CategoryCreateResponse> create(@Valid @RequestBody CategoryCreateRequest categoryCreateRequest) {
        return ApiResponse.ok(categoryService.createCategory(categoryCreateRequest));
    }
}
