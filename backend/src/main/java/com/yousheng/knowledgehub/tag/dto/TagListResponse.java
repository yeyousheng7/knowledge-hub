package com.yousheng.knowledgehub.tag.dto;

import java.util.List;

public record TagListResponse(
        List<TagListItemResponse> items
) {
}