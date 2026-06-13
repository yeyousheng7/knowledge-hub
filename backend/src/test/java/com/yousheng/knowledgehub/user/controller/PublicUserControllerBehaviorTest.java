package com.yousheng.knowledgehub.user.controller;

import com.yousheng.knowledgehub.support.ControllerBehaviorTestSupport;
import com.yousheng.knowledgehub.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicUserControllerBehaviorTest extends ControllerBehaviorTestSupport {

    @Test
    void public_getUserProfile_success() throws Exception {
        AppUser user = createEnabledUser("testuser", "Test User", "USER");

        mockMvc.perform(get("/api/v1/public/users/{username}", user.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.nickname").value("Test User"))
                .andExpect(jsonPath("$.data.bio").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    void public_getUserProfile_userNotFound_returns40402() throws Exception {
        mockMvc.perform(get("/api/v1/public/users/{username}", "nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));
    }

    @Test
    void public_getUserProfile_disabledUser_returns40402() throws Exception {
        AppUser user = createDisabledUser("disableduser", "Disabled User", "USER");

        mockMvc.perform(get("/api/v1/public/users/{username}", user.getUsername()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));
    }

    @Test
    void public_getUserProfile_invalidUsername_returns40001() throws Exception {
        mockMvc.perform(get("/api/v1/public/users/{username}", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    // ---- listUserPublicNotes ----

    @Test
    void public_listUserPublicNotes_success() throws Exception {
        AppUser user = createEnabledUser("author1", "Author One", "USER");

        insertPublicNote(user.getId(), "My Public Note", "content", "summary",
                LocalDateTime.of(2026, 6, 3, 10, 0));

        mockMvc.perform(get("/api/v1/public/users/{username}/notes", user.getUsername())
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("My Public Note"));
    }

    @Test
    void public_listUserPublicNotes_userNotFound_returns40402() throws Exception {
        mockMvc.perform(get("/api/v1/public/users/{username}/notes", "nonexistent")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));
    }

    @Test
    void public_listUserPublicNotes_disabledUser_returns40402() throws Exception {
        AppUser user = createDisabledUser("disabledauthor", "Disabled Author", "USER");

        mockMvc.perform(get("/api/v1/public/users/{username}/notes", user.getUsername())
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));
    }

    @Test
    void public_listUserPublicNotes_onlyReturnsThatAuthorPublicNormalPublishedNotes() throws Exception {
        AppUser author = createEnabledUser("targetauthor", "Target Author", "USER");
        AppUser other = createEnabledUser("otherauthor", "Other Author", "USER");

        // target's public note (should appear)
        insertPublicNote(author.getId(), "Target Public", "content", "summary",
                LocalDateTime.of(2026, 6, 3, 11, 0));

        // target's private note (should NOT appear)
        insertNote(author.getId(), "Target Private", "private content", "private summary");

        // target's deleted public note (should NOT appear)
        Long deletedId = insertNote(author.getId(), "Target Deleted", "deleted content", "deleted summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ?, deleted = 1, deleted_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 11, 5), LocalDateTime.now(), deletedId);

        // target's taken-down public note (should NOT appear)
        Long takenDownId = insertPublicNote(author.getId(), "Target Taken Down", "content", "summary",
                LocalDateTime.of(2026, 6, 3, 11, 10));
        jdbcTemplate.update(
                "UPDATE note SET moderation_status = 'TAKEN_DOWN' WHERE id = ?", takenDownId);

        // other author's public note (should NOT appear)
        insertPublicNote(other.getId(), "Other Public", "other content", "other summary",
                LocalDateTime.of(2026, 6, 3, 11, 15));

        mockMvc.perform(get("/api/v1/public/users/{username}/notes", author.getUsername())
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Target Public"));
    }

    @Test
    void public_listUserPublicNotes_paginationWorks() throws Exception {
        AppUser author = createEnabledUser("pageauthor", "Page Author", "USER");

        insertPublicNote(author.getId(), "Note 1", "content", "summary",
                LocalDateTime.of(2026, 6, 3, 12, 0));
        insertPublicNote(author.getId(), "Note 2", "content", "summary",
                LocalDateTime.of(2026, 6, 3, 12, 1));
        insertPublicNote(author.getId(), "Note 3", "content", "summary",
                LocalDateTime.of(2026, 6, 3, 12, 2));

        mockMvc.perform(get("/api/v1/public/users/{username}/notes", author.getUsername())
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2));
    }

    @Test
    void public_listUserPublicNotes_invalidUsername_returns40001() throws Exception {
        mockMvc.perform(get("/api/v1/public/users/{username}/notes", "ab")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    // ---- helpers ----

    private Long insertNote(Long userId, String title, String contentMd, String summary) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO note (
                                user_id, title, content_md, summary, visibility, category_id,
                                created_at, updated_at, published_at, moderation_status,
                                moderated_at, deleted, deleted_at
                        ) VALUES (?, ?, ?, ?, 'PRIVATE', NULL, ?, ?, NULL, 'NORMAL', NULL, 0, NULL)
                        """,
                userId, title, contentMd, summary, now, now
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertPublicNote(Long userId, String title, String contentMd, String summary,
                                  LocalDateTime publishedAt) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO note (
                                user_id, title, content_md, summary, visibility, category_id,
                                created_at, updated_at, published_at, moderation_status,
                                moderated_at, deleted, deleted_at
                        ) VALUES (?, ?, ?, ?, 'PUBLIC', NULL, ?, ?, ?, 'NORMAL', NULL, 0, NULL)
                        """,
                userId, title, contentMd, summary, now, now, publishedAt
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
