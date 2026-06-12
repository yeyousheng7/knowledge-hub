package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.support.ControllerBehaviorTestSupport;

import java.time.LocalDateTime;

abstract class AbstractControllerBehaviorTest extends ControllerBehaviorTestSupport {

    protected Long insertCategory(Long userId, String name) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO category (user_id, name, created_at, updated_at, deleted, deleted_marker, deleted_at)
                        VALUES (?, ?, ?, ?, 0, 0, NULL)
                        """,
                userId,
                name,
                now,
                now
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    protected Long insertTag(Long userId, String name) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO tag (user_id, name, created_at, updated_at, deleted, deleted_marker, deleted_at) VALUES (?, ?, ?, ?, 0, 0, NULL)",
                userId,
                name,
                now,
                now
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    protected Long createDeletedNote(Long userId, String title, String contentMd, String summary) {
        return insertNote(userId, title, contentMd, summary, 1, LocalDateTime.now());
    }

    protected Long insertNote(
            Long userId,
            String title,
            String contentMd,
            String summary,
            int deleted,
            LocalDateTime deletedAt
    ) {
        LocalDateTime now = LocalDateTime.now();
        return insertNote(userId, title, contentMd, summary, now, now, deleted, deletedAt);
    }

    protected Long insertNote(
            Long userId,
            String title,
            String contentMd,
            String summary,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int deleted,
            LocalDateTime deletedAt
    ) {
        return insertNote(userId, title, contentMd, summary, createdAt, updatedAt, deleted, deletedAt, null);
    }

    protected Long insertNote(
            Long userId,
            String title,
            String contentMd,
            String summary,
            int deleted,
            LocalDateTime deletedAt,
            Long categoryId
    ) {
        LocalDateTime now = LocalDateTime.now();
        return insertNote(userId, title, contentMd, summary, now, now, deleted, deletedAt, categoryId);
    }

    protected Long insertNote(
            Long userId,
            String title,
            String contentMd,
            String summary,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int deleted,
            LocalDateTime deletedAt,
            Long categoryId
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO note (
                                user_id, title, content_md, summary, visibility, category_id,
                                created_at, updated_at, published_at, moderation_status,
                                moderated_at, deleted, deleted_at
                        ) VALUES (?, ?, ?, ?, 'PRIVATE', ?, ?, ?, NULL, 'NORMAL', NULL, ?, ?)
                        """,
                userId,
                title,
                contentMd,
                summary,
                categoryId,
                createdAt,
                updatedAt,
                deleted,
                deletedAt
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
