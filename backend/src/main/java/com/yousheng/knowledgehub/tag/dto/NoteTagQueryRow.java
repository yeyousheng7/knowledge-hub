package com.yousheng.knowledgehub.tag.dto;

public record NoteTagQueryRow(
        Long noteId,
        Long tagId,
        String tagName
) {
}
