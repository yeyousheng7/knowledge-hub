package com.yousheng.knowledgehub.admin.dto;

public record AdminNoteAuthorResponse(
        Long userId,
        String username,
        String nickname,
        String status
) {
}
