package com.yousheng.knowledgehub.tag.controller;

import com.yousheng.knowledgehub.security.JwtConstants;
import com.yousheng.knowledgehub.support.ControllerBehaviorTestSupport;
import com.yousheng.knowledgehub.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TagControllerBehaviorTest extends ControllerBehaviorTestSupport {

    // ---- createTag ----

    @Test
    void createTag_withValidToken_returnsCreatedTag() throws Exception {
        AppUser user = createEnabledUser("tag_user_1", "TagUser", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "name": "  Java  "
                }
                """;

        mockMvc.perform(post("/api/v1/tags")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("Java"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tag WHERE user_id = ? AND name = ? AND deleted = 0",
                Integer.class,
                user.getId(),
                "Java"
        );

        assertEquals(1, count);
    }

    @Test
    void createTag_withDuplicateName_returns409() throws Exception {
        AppUser user = createEnabledUser("tag_user_2", "TagUser2", "USER");
        String token = tokenOf(user);

        insertTag(user.getId(), "Spring");

        String body = """
                {
                  "name": "Spring"
                }
                """;

        mockMvc.perform(post("/api/v1/tags")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40903));
    }

    @Test
    void createTag_withBlankName_returns400() throws Exception {
        AppUser user = createEnabledUser("tag_user_3", "TagUser3", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "name": "   "
                }
                """;

        mockMvc.perform(post("/api/v1/tags")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void createTag_withoutToken_returns401() throws Exception {
        String body = """
                {
                  "name": "Unauth"
                }
                """;

        mockMvc.perform(post("/api/v1/tags")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    // ---- listTags ----

    @Test
    void listTags_withValidToken_returnsOwnTags() throws Exception {
        AppUser user = createEnabledUser("tag_user_4", "TagUser4", "USER");
        String token = tokenOf(user);

        insertTag(user.getId(), "Spring");
        insertTag(user.getId(), "Java");

        mockMvc.perform(get("/api/v1/tags")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].name").value("Java"))
                .andExpect(jsonPath("$.data.items[1].name").value("Spring"));
    }

    // ---- updateTag ----

    @Test
    void updateTag_withValidToken_updatesName() throws Exception {
        AppUser user = createEnabledUser("tag_upd_1", "TagUpd1", "USER");
        String token = tokenOf(user);

        Long tagId = insertTag(user.getId(), "Old Name");

        String body = """
                {
                  "name": "  New Name  "
                }
                """;

        mockMvc.perform(put("/api/v1/tags/{tagId}", tagId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(tagId.intValue()))
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());

        String dbName = jdbcTemplate.queryForObject(
                "SELECT name FROM tag WHERE id = ? AND deleted = 0", String.class, tagId);
        assertEquals("New Name", dbName);
    }

    @Test
    void updateTag_withDuplicateName_returns409() throws Exception {
        AppUser user = createEnabledUser("tag_upd_dup", "TagUpdDup", "USER");
        String token = tokenOf(user);

        insertTag(user.getId(), "Spring");
        Long tagId = insertTag(user.getId(), "Java");

        String body = """
                {
                  "name": "Spring"
                }
                """;

        mockMvc.perform(put("/api/v1/tags/{tagId}", tagId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40903));
    }

    @Test
    void updateTag_withOtherUserTag_returns404() throws Exception {
        AppUser owner = createEnabledUser("tag_upd_owner", "TagUpdOwner", "USER");
        AppUser other = createEnabledUser("tag_upd_other", "TagUpdOther", "USER");
        String otherToken = tokenOf(other);

        Long ownerTagId = insertTag(owner.getId(), "Owner Tag");

        String body = """
                {
                  "name": "Hacked"
                }
                """;

        mockMvc.perform(put("/api/v1/tags/{tagId}", ownerTagId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + otherToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40404));
    }

    // ---- deleteTag ----

    @Test
    void deleteTag_withValidToken_deletesTag() throws Exception {
        AppUser user = createEnabledUser("tag_del_1", "TagDel1", "USER");
        String token = tokenOf(user);

        Long tagId = insertTag(user.getId(), "To Delete");

        mockMvc.perform(delete("/api/v1/tags/{tagId}", tagId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证删除后标签列表不再返回该标签
        mockMvc.perform(get("/api/v1/tags")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void deleteTag_withOtherUserTag_returns404() throws Exception {
        AppUser owner = createEnabledUser("tag_del_owner", "TagDelOwner", "USER");
        AppUser other = createEnabledUser("tag_del_other", "TagDelOther", "USER");
        String otherToken = tokenOf(other);

        Long ownerTagId = insertTag(owner.getId(), "Owner Tag");

        mockMvc.perform(delete("/api/v1/tags/{tagId}", ownerTagId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40404));
    }

    @Test
    void deleteTag_withBoundNotes_removesNoteTagRelations() throws Exception {
        AppUser user = createEnabledUser("tag_del_note", "TagDelNote", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Note With Tag", "content", "summary");
        Long tagId = insertTag(user.getId(), "To Delete");

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                noteId, tagId, now
        );

        mockMvc.perform(delete("/api/v1/tags/{tagId}", tagId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // tag 列表不再返回该 tag
        mockMvc.perform(get("/api/v1/tags")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));

        // note_tag 中对应关系数量为 0
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM note_tag WHERE tag_id = ?", Integer.class, tagId);
        assertEquals(0, count);
    }

    // ---- helpers ----

    private Long insertNote(Long userId, String title, String contentMd, String summary) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO note (user_id, title, content_md, summary, visibility,
                            created_at, updated_at, published_at, moderation_status,
                            moderated_at, deleted, deleted_at)
                        VALUES (?, ?, ?, ?, 'PRIVATE', ?, ?, NULL, 'NORMAL', NULL, 0, NULL)
                        """,
                userId, title, contentMd, summary, now, now
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertTag(Long userId, String name) {
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
}
