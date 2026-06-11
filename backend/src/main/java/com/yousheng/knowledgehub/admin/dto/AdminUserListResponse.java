package com.yousheng.knowledgehub.admin.dto;

import java.util.List;

public record AdminUserListResponse(
        List<AdminUserItemResponse> items,
        long total,
        long page,
        long size
) {
}
