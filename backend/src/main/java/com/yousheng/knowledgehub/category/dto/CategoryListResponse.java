package com.yousheng.knowledgehub.category.dto;

import java.util.List;

public record CategoryListResponse(
        List<CategoryListItemResponse> items
) {
}