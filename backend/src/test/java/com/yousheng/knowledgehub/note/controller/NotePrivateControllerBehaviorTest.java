package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.security.JwtConstants;
import com.yousheng.knowledgehub.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotePrivateControllerBehaviorTest extends AbstractControllerBehaviorTest {

    @Test
    void createNote_withTokenAndTitle_returns200() throws Exception {
        AppUser user = createEnabledUser("note_user_1", "NoteUser", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "title": "  First Note  ",
                  "contentMd": "# hello",
                  "summary": "summary text"
                }
                """;

        mockMvc.perform(post("/api/v1/notes")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("First Note"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.moderationStatus").value("NORMAL"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM note
                        WHERE user_id = ?
                          AND title = ?
                          AND content_md = ?
                          AND summary = ?
                          AND visibility = 'PRIVATE'
                          AND moderation_status = 'NORMAL'
                          AND deleted = 0
                        """,
                Integer.class,
                user.getId(),
                "First Note",
                "# hello",
                "summary text"
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    void createNote_withoutToken_returns401() throws Exception {
        String body = """
                {
                  "title": "Unauth Note",
                  "contentMd": "# hello",
                  "summary": "summary text"
                }
                """;

        mockMvc.perform(post("/api/v1/notes")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createNote_withTokenWithoutTitle_returns40001() throws Exception {
        AppUser user = createEnabledUser("note_user_2", "NoteUser2", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "contentMd": "# hello",
                  "summary": "summary text"
                }
                """;

        mockMvc.perform(post("/api/v1/notes")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void createNote_withDisabledUserAndValidToken_returns40301() throws Exception {
        AppUser user = createDisabledUser("note_user_3", "NoteUser3", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "title": "Disabled Note",
                  "contentMd": "# hello",
                  "summary": "summary text"
                }
                """;

        mockMvc.perform(post("/api/v1/notes")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void createNote_withTooLongContentMd_returns40001() throws Exception {
        AppUser user = createEnabledUser("note_user_long", "NoteUserLong", "USER");
        String token = tokenOf(user);

        String longContent = "a".repeat(100_001);

        String body = """
                {
                  "title": "Too Long Content",
                  "contentMd": "%s"
                }
                """.formatted(longContent);

        mockMvc.perform(post("/api/v1/notes")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void createNote_withValidCategory_returnsCategoryId() throws Exception {
        AppUser user = createEnabledUser("note_cat_valid", "NoteCatValid", "USER");
        String token = tokenOf(user);
        Long categoryId = insertCategory(user.getId(), "Tech");

        String body = """
                {
                  "title": "Note With Category",
                  "contentMd": "# hello",
                  "summary": "summary text",
                  "categoryId": %d
                }
                """.formatted(categoryId);

        mockMvc.perform(post("/api/v1/notes")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Note With Category"))
                .andExpect(jsonPath("$.data.categoryId").value(categoryId.intValue()))
                .andExpect(jsonPath("$.data.id").exists());

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM note
                        WHERE user_id = ?
                          AND title = ?
                          AND category_id = ?
                          AND deleted = 0
                        """,
                Integer.class,
                user.getId(),
                "Note With Category",
                categoryId
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void createNote_withOtherUserCategory_returns404() throws Exception {
        AppUser owner = createEnabledUser("note_cat_owner", "NoteCatOwner", "USER");
        AppUser other = createEnabledUser("note_cat_other", "NoteCatOther", "USER");
        String otherToken = tokenOf(other);
        Long categoryId = insertCategory(owner.getId(), "Owner Category");

        String body = """
                {
                  "title": "Steal Category",
                  "contentMd": "# hello",
                  "summary": "summary text",
                  "categoryId": %d
                }
                """.formatted(categoryId);

        mockMvc.perform(post("/api/v1/notes")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + otherToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40403));
    }

    @Test
    void listNotes_withToken_returnsOwnActiveNotesInOrder() throws Exception {
        AppUser owner = createEnabledUser("note_list_owner", "NoteListOwner", "USER");
        AppUser otherUser = createEnabledUser("note_list_other", "NoteListOther", "USER");
        String token = tokenOf(owner);

        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 12, 0);
        LocalDateTime deletedTime = baseTime.plusMinutes(50);

        insertNote(owner.getId(), "Active Note Latest", "content latest", "summary latest", baseTime.plusMinutes(30), baseTime.plusMinutes(30), 0, null);
        insertNote(otherUser.getId(), "Other User Note", "other content", "other summary", baseTime.plusMinutes(40), baseTime.plusMinutes(40), 0, null);
        insertNote(owner.getId(), "Deleted Own Note", "deleted content", "deleted summary", baseTime.plusMinutes(50), deletedTime, 1, deletedTime);
        insertNote(owner.getId(), "Active Note Tie First", "content tie first", "summary tie first", baseTime.plusMinutes(20), baseTime.plusMinutes(20), 0, null);
        insertNote(owner.getId(), "Active Note Tie Second", "content tie second", "summary tie second", baseTime.plusMinutes(20), baseTime.plusMinutes(20), 0, null);
        insertNote(owner.getId(), "Active Note Oldest", "content oldest", "summary oldest", baseTime.plusMinutes(10), baseTime.plusMinutes(10), 0, null);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.items.length()").value(4))
                .andExpect(jsonPath("$.data.items[0].title").value("Active Note Latest"))
                .andExpect(jsonPath("$.data.items[1].title").value("Active Note Tie Second"))
                .andExpect(jsonPath("$.data.items[2].title").value("Active Note Tie First"))
                .andExpect(jsonPath("$.data.items[3].title").value("Active Note Oldest"));
    }

    @Test
    void listNotes_withInvalidPageOrSize_returns40001() throws Exception {
        AppUser user = createEnabledUser("note_list_invalid_param", "NoteListInvalidParam", "USER");
        String token = tokenOf(user);

        mockMvc.perform(get("/api/v1/notes?page=0&size=20")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));

        mockMvc.perform(get("/api/v1/notes?page=1&size=101")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void listNotes_withDisabledUser_returns40301() throws Exception {
        AppUser user = createDisabledUser("note_list_disabled", "NoteListDisabled", "USER");
        String token = tokenOf(user);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void listNotes_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notes?page=1&size=20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listNotes_withTagId_returnsNotesBoundToTag() throws Exception {
        AppUser user = createEnabledUser("note_list_tag", "NoteListTag", "USER");
        String token = tokenOf(user);
        Long tagId = insertTag(user.getId(), "Spring");

        Long boundId1 = insertNote(user.getId(), "Spring Note 1", "content", "summary", 0, null);
        Long boundId2 = insertNote(user.getId(), "Spring Note 2", "content", "summary", 0, null);
        insertNote(user.getId(), "Java Note", "content", "summary", 0, null);

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                boundId1, tagId, now);
        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                boundId2, tagId, now);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&tagId=" + tagId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].title").value("Spring Note 2"))
                .andExpect(jsonPath("$.data.items[1].title").value("Spring Note 1"));
    }

    @Test
    void listNotes_withTagIdButNoBoundNotes_returnsEmptyList() throws Exception {
        AppUser user = createEnabledUser("note_list_tag_empty", "NoteListTagEmpty", "USER");
        String token = tokenOf(user);
        Long tagId = insertTag(user.getId(), "Unused Tag");

        insertNote(user.getId(), "Some Note", "content", "summary", 0, null);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&tagId=" + tagId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void listNotes_withOtherUserTag_returns404() throws Exception {
        AppUser owner = createEnabledUser("note_list_tag_owner", "NoteListTagOwner", "USER");
        AppUser other = createEnabledUser("note_list_tag_other", "NoteListTagOther", "USER");
        String otherToken = tokenOf(other);
        Long ownerTagId = insertTag(owner.getId(), "Owner Tag");

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&tagId=" + ownerTagId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40404));
    }

    @Test
    void listNotes_withCategoryIdAndTagId_returnsIntersection() throws Exception {
        AppUser user = createEnabledUser("note_list_cat_tag", "NoteListCatTag", "USER");
        String token = tokenOf(user);
        Long catId = insertCategory(user.getId(), "Tech");
        Long tagId = insertTag(user.getId(), "Java");

        Long matchId = insertNote(user.getId(), "Java In Tech", "content", "summary", 0, null, catId);
        insertNote(user.getId(), "Java No Cat", "content", "summary", 0, null);
        insertNote(user.getId(), "Spring In Tech", "content", "summary", 0, null, catId);

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                matchId, tagId, now);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&categoryId=" + catId + "&tagId=" + tagId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Java In Tech"));
    }

    @Test
    void listNotes_withCategoryId_returnsNotesInCategory() throws Exception {
        AppUser user = createEnabledUser("note_list_cat", "NoteListCat", "USER");
        String token = tokenOf(user);
        Long catA = insertCategory(user.getId(), "Category A");
        Long catB = insertCategory(user.getId(), "Category B");

        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 12, 0);
        insertNote(user.getId(), "Note A1", "content a1", "summary a1", baseTime.plusMinutes(10), baseTime.plusMinutes(10), 0, null, catA);
        insertNote(user.getId(), "Note A2", "content a2", "summary a2", baseTime.plusMinutes(20), baseTime.plusMinutes(20), 0, null, catA);
        insertNote(user.getId(), "Note B1", "content b1", "summary b1", baseTime.plusMinutes(30), baseTime.plusMinutes(30), 0, null, catB);
        insertNote(user.getId(), "Note No Cat", "content none", "summary none", baseTime.plusMinutes(40), baseTime.plusMinutes(40), 0, null, null);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&categoryId=" + catA)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].title").value("Note A2"))
                .andExpect(jsonPath("$.data.items[1].title").value("Note A1"));
    }

    @Test
    void listNotes_withOtherUserCategory_returns404() throws Exception {
        AppUser owner = createEnabledUser("note_list_cat_owner", "NoteListCatOwner", "USER");
        AppUser other = createEnabledUser("note_list_cat_other", "NoteListCatOther", "USER");
        String otherToken = tokenOf(other);
        Long ownerCategoryId = insertCategory(owner.getId(), "Owner Category");

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&categoryId=" + ownerCategoryId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40403));
    }

    @Test
    void listNotes_mixedTaggedAndUntagged_returnsTagsOrEmptyArray() throws Exception {
        AppUser user = createEnabledUser("note_list_tag_mix", "NoteListTagMix", "USER");
        String token = tokenOf(user);

        Long untaggedId = insertNote(user.getId(), "Untagged Note", "content", "summary", 0, null);
        Long taggedId = insertNote(user.getId(), "Tagged Note", "content", "summary", 0, null);
        Long tagId = insertTag(user.getId(), "Java");

        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                taggedId, tagId, LocalDateTime.now());

        mockMvc.perform(get("/api/v1/notes?page=1&size=20")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                // tagged note comes first (newer by id)
                .andExpect(jsonPath("$.data.items[0].title").value("Tagged Note"))
                .andExpect(jsonPath("$.data.items[0].tags.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].tags[0].id").value(tagId.intValue()))
                .andExpect(jsonPath("$.data.items[0].tags[0].name").value("Java"))
                // untagged note returns empty array
                .andExpect(jsonPath("$.data.items[1].title").value("Untagged Note"))
                .andExpect(jsonPath("$.data.items[1].tags.length()").value(0));
    }

    @Test
    void listNotes_withoutCategoryId_returnsAllMyNotes() throws Exception {
        AppUser user = createEnabledUser("note_list_all", "NoteListAll", "USER");
        String token = tokenOf(user);
        Long catA = insertCategory(user.getId(), "Category A");

        insertNote(user.getId(), "Note In Cat", "content cat", "summary cat", 0, null, catA);
        insertNote(user.getId(), "Note No Cat", "content none", "summary none", 0, null, null);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void getMyNoteDetail_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notes/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyNoteDetail_ownNote_returns200() throws Exception {
        AppUser user = createEnabledUser("note_detail_owner", "NoteDetailOwner", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Detail Note", "# detail content", "detail summary", 0, null);

        mockMvc.perform(get("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(noteId.intValue()))
                .andExpect(jsonPath("$.data.title").value("Detail Note"))
                .andExpect(jsonPath("$.data.contentMd").value("# detail content"))
                .andExpect(jsonPath("$.data.summary").value("detail summary"));
    }

    @Test
    void getMyNoteDetail_notExists_returns40401() throws Exception {
        AppUser user = createEnabledUser("note_detail_missing", "NoteDetailMissing", "USER");
        String token = tokenOf(user);

        mockMvc.perform(get("/api/v1/notes/{noteId}", 999_999L)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void getMyNoteDetail_otherUsersNote_returns40401() throws Exception {
        AppUser owner = createEnabledUser("note_detail_owner_2", "NoteDetailOwner2", "USER");
        AppUser viewer = createEnabledUser("note_detail_viewer", "NoteDetailViewer", "USER");
        String viewerToken = tokenOf(viewer);

        Long noteId = insertNote(owner.getId(), "Other User Note", "other content", "other summary", 0, null);

        mockMvc.perform(get("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + viewerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void getMyNoteDetail_deletedNote_returns40401() throws Exception {
        AppUser user = createEnabledUser("note_detail_deleted", "NoteDetailDeleted", "USER");
        String token = tokenOf(user);

        Long noteId = createDeletedNote(user.getId(), "Deleted Note", "deleted content", "deleted summary");

        mockMvc.perform(get("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void getMyNoteDetail_disabledUserWithValidToken_returns40301() throws Exception {
        AppUser user = createDisabledUser("note_detail_disabled", "NoteDetailDisabled", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Disabled User Note", "disabled content", "disabled summary", 0, null);

        mockMvc.perform(get("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void updateNote_withToken_updatesOwnNote_returns200() throws Exception {
        AppUser user = createEnabledUser("note_update_owner", "NoteUpdateOwner", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Old Title", "old content", "old summary", 0, null);
        LocalDateTime originalUpdatedAt = jdbcTemplate.queryForObject("SELECT updated_at FROM note WHERE id = ?", LocalDateTime.class, noteId);

        Thread.sleep(100);

        String body = """
                {
                  "title": "  New Title  ",
                  "contentMd": "# new content",
                  "summary": "new summary"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(noteId.intValue()))
                .andExpect(jsonPath("$.data.title").value("New Title"))
                .andExpect(jsonPath("$.data.contentMd").value("# new content"))
                .andExpect(jsonPath("$.data.summary").value("new summary"));

        String dbTitle = jdbcTemplate.queryForObject("SELECT title FROM note WHERE id = ?", String.class, noteId);
        String dbContent = jdbcTemplate.queryForObject("SELECT content_md FROM note WHERE id = ?", String.class, noteId);
        String dbSummary = jdbcTemplate.queryForObject("SELECT summary FROM note WHERE id = ?", String.class, noteId);
        LocalDateTime updatedUpdatedAt = jdbcTemplate.queryForObject("SELECT updated_at FROM note WHERE id = ?", LocalDateTime.class, noteId);

        assertThat(dbTitle).isEqualTo("New Title");
        assertThat(dbContent).isEqualTo("# new content");
        assertThat(dbSummary).isEqualTo("new summary");
        assertThat(updatedUpdatedAt).isAfter(originalUpdatedAt);
    }

    @Test
    void updateNote_withoutToken_returns401() throws Exception {
        Long dummyId = 1L;
        String body = """
                {
                  "title": "New Title",
                  "contentMd": "# new content"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/{noteId}", dummyId)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateNote_otherUsersNote_returns40401() throws Exception {
        AppUser owner = createEnabledUser("note_update_owner2", "NoteUpdateOwner2", "USER");
        AppUser viewer = createEnabledUser("note_update_viewer", "NoteUpdateViewer", "USER");
        String viewerToken = tokenOf(viewer);

        Long noteId = insertNote(owner.getId(), "Owner Note", "owner content", "owner summary", 0, null);

        String body = """
                {
                  "title": "Hacker Title",
                  "contentMd": "# hacked"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + viewerToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void updateNote_deletedNote_returns40401() throws Exception {
        AppUser user = createEnabledUser("note_update_deleted", "NoteUpdateDeleted", "USER");
        String token = tokenOf(user);

        Long noteId = createDeletedNote(user.getId(), "Deleted Note", "deleted content", "deleted summary");

        String body = """
                {
                  "title": "New Title",
                  "contentMd": "# new content"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void updateNote_emptyTitle_returns40001() throws Exception {
        AppUser user = createEnabledUser("note_update_empty_title", "NoteUpdateEmptyTitle", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Some Title", "content", "summary", 0, null);

        String body = """
                {
                  "title": "   ",
                  "contentMd": "# new content"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void updateNote_tooLongContent_returns40001() throws Exception {
        AppUser user = createEnabledUser("note_update_too_long", "NoteUpdateTooLong", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Some Title", "content", "summary", 0, null);

        String longContent = "a".repeat(100_001);

        String body = """
                {
                  "title": "Valid Title",
                  "contentMd": "%s"
                }
                """.formatted(longContent);

        mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void updateNote_withDisabledUser_returns40301() throws Exception {
        AppUser user = createDisabledUser("note_update_disabled", "NoteUpdateDisabled", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Disabled Note", "disabled content", "disabled summary", 0, null);

        String body = """
                {
                  "title": "New Title",
                  "contentMd": "# new content"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void updateNote_withValidCategory_returnsCategoryId() throws Exception {
        AppUser user = createEnabledUser("note_upd_cat_valid", "NoteUpdCatValid", "USER");
        String token = tokenOf(user);
        Long noteId = insertNote(user.getId(), "Old Title", "old content", "old summary", 0, null);
        Long categoryId = insertCategory(user.getId(), "Update Category");

        String body = """
                {
                  "title": "Updated Title",
                  "contentMd": "# updated",
                  "categoryId": %d
                }
                """.formatted(categoryId);

        mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.categoryId").value(categoryId.intValue()));

        Long dbCategoryId = jdbcTemplate.queryForObject(
                "SELECT category_id FROM note WHERE id = ?", Long.class, noteId);
        assertThat(dbCategoryId).isEqualTo(categoryId);
    }

    @Test
    void updateNote_withNullCategory_clearsCategory() throws Exception {
        AppUser user = createEnabledUser("note_upd_clear_cat", "NoteUpdClearCat", "USER");
        String token = tokenOf(user);
        Long categoryId = insertCategory(user.getId(), "To Be Cleared");
        Long noteId = insertNote(user.getId(), "Note With Cat", "content", "summary", 0, null, categoryId);

        String body = """
                {
                  "title": "Category Cleared",
                  "contentMd": "# cleared",
                  "categoryId": null
                }
                """;

        mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Long dbCategoryId = jdbcTemplate.queryForObject(
                "SELECT category_id FROM note WHERE id = ?", Long.class, noteId);
        assertThat(dbCategoryId).isNull();
    }

    @Test
    void updateNote_noteNotAccessible_withInvalidCategory_returns40401() throws Exception {
        AppUser owner = createEnabledUser("note_upd_nf_owner", "NoteUpdNfOwner", "USER");
        AppUser other = createEnabledUser("note_upd_nf_other", "NoteUpdNfOther", "USER");
        String otherToken = tokenOf(other);
        Long ownerCategoryId = insertCategory(owner.getId(), "Owner Category");

        String body = """
                {
                  "title": "Hack Attempt",
                  "contentMd": "# hacked",
                  "categoryId": %d
                }
                """.formatted(ownerCategoryId);

        // non-existent note + invalid category → NOTE_NOT_FOUND, not CATEGORY_NOT_FOUND
        mockMvc.perform(put("/api/v1/notes/{noteId}", 999_999L)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + otherToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        // other user's note + invalid category → NOTE_NOT_FOUND
        Long ownerNoteId = insertNote(owner.getId(), "Owner Note", "owner content", "owner summary", 0, null);
        mockMvc.perform(put("/api/v1/notes/{noteId}", ownerNoteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + otherToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        // deleted note + invalid category → NOTE_NOT_FOUND
        Long deletedNoteId = createDeletedNote(other.getId(), "Deleted Note", "deleted content", "deleted summary");
        mockMvc.perform(put("/api/v1/notes/{noteId}", deletedNoteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + otherToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void deleteNote_withToken_deletesOwnNote_returns200() throws Exception {
        AppUser user = createEnabledUser("note_delete_owner", "NoteDeleteOwner", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Delete Me", "delete content", "delete summary", 0, null);

        mockMvc.perform(delete("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Integer deleted = jdbcTemplate.queryForObject("SELECT deleted FROM note WHERE id = ?", Integer.class, noteId);
        LocalDateTime deletedAt = jdbcTemplate.queryForObject("SELECT deleted_at FROM note WHERE id = ?", LocalDateTime.class, noteId);

        assertThat(deleted).isEqualTo(1);
        assertThat(deletedAt).isNotNull();
    }

    @Test
    void deleteNote_withoutToken_returns401() throws Exception {
        Long noteId = insertNote(createEnabledUser("note_delete_unauth", "NoteDeleteUnauth", "USER").getId(), "Delete Me", "delete content", "delete summary", 0, null);

        mockMvc.perform(delete("/api/v1/notes/{noteId}", noteId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteNote_otherUsersNote_returns40401() throws Exception {
        AppUser owner = createEnabledUser("note_delete_owner_2", "NoteDeleteOwner2", "USER");
        AppUser viewer = createEnabledUser("note_delete_viewer", "NoteDeleteViewer", "USER");
        String viewerToken = tokenOf(viewer);

        Long noteId = insertNote(owner.getId(), "Owner Delete Note", "owner content", "owner summary", 0, null);

        mockMvc.perform(delete("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + viewerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void deleteNote_deletedNote_returns40401() throws Exception {
        AppUser user = createEnabledUser("note_delete_deleted", "NoteDeleteDeleted", "USER");
        String token = tokenOf(user);

        Long noteId = createDeletedNote(user.getId(), "Deleted Note", "deleted content", "deleted summary");

        mockMvc.perform(delete("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void deleteNote_withTags_removesNoteTagRelations() throws Exception {
        AppUser user = createEnabledUser("note_del_tag", "NoteDelTag", "USER");
        String token = tokenOf(user);
        Long noteId = insertNote(user.getId(), "Note With Tags To Delete", "content", "summary", 0, null);
        Long tagId1 = insertTag(user.getId(), "Java");
        Long tagId2 = insertTag(user.getId(), "Spring");

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                noteId, tagId1, now);
        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                noteId, tagId2, now);

        mockMvc.perform(delete("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM note WHERE id = ?", Integer.class, noteId);
        assertThat(deleted).isEqualTo(1);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM note_tag WHERE note_id = ?", Integer.class, noteId);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void deleteNote_withDisabledUser_returns40301() throws Exception {
        AppUser user = createDisabledUser("note_delete_disabled", "NoteDeleteDisabled", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Disabled Delete Note", "disabled content", "disabled summary", 0, null);

        mockMvc.perform(delete("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void deleteNote_thenDetail_returns40401() throws Exception {
        AppUser user = createEnabledUser("note_delete_detail", "NoteDeleteDetail", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Detail Gone", "detail content", "detail summary", 0, null);

        mockMvc.perform(delete("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void deleteNote_thenListDoesNotReturnDeletedNote() throws Exception {
        AppUser user = createEnabledUser("note_delete_list", "NoteDeleteList", "USER");
        String token = tokenOf(user);

        Long deletedNoteId = insertNote(user.getId(), "Deleted In List", "deleted content", "deleted summary", 0, null);
        insertNote(user.getId(), "Remaining Note", "remaining content", "remaining summary", 0, null);

        mockMvc.perform(delete("/api/v1/notes/{noteId}", deletedNoteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notes?page=1&size=20")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Remaining Note"));
    }

    // ---- createNote with tagIds ----

    @Test
    void createNote_withValidTagIds_returnsTags() throws Exception {
        AppUser user = createEnabledUser("note_tag_create", "NoteTagCreate", "USER");
        String token = tokenOf(user);
        Long tagId1 = insertTag(user.getId(), "Java");
        Long tagId2 = insertTag(user.getId(), "Spring");

        String body = """
                {
                  "title": "Note With Tags",
                  "contentMd": "# hello",
                  "summary": "summary",
                  "tagIds": [%d, %d]
                }
                """.formatted(tagId1, tagId2);

        mockMvc.perform(post("/api/v1/notes")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Note With Tags"))
                .andExpect(jsonPath("$.data.tags.length()").value(2))
                .andExpect(jsonPath("$.data.tags[0].id").value(tagId1.intValue()))
                .andExpect(jsonPath("$.data.tags[0].name").value("Java"))
                .andExpect(jsonPath("$.data.tags[1].id").value(tagId2.intValue()))
                .andExpect(jsonPath("$.data.tags[1].name").value("Spring"));

        Long noteId = jdbcTemplate.queryForObject(
                "SELECT id FROM note WHERE user_id = ? AND title = ? AND deleted = 0",
                Long.class, user.getId(), "Note With Tags");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM note_tag WHERE note_id = ?", Integer.class, noteId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void createNote_withOtherUserTag_returns404() throws Exception {
        AppUser owner = createEnabledUser("note_tag_owner", "NoteTagOwner", "USER");
        AppUser other = createEnabledUser("note_tag_other", "NoteTagOther", "USER");
        String otherToken = tokenOf(other);
        Long ownerTagId = insertTag(owner.getId(), "Owner Tag");

        String body = """
                {
                  "title": "Steal Tag",
                  "contentMd": "# hello",
                  "summary": "summary",
                  "tagIds": [%d]
                }
                """.formatted(ownerTagId);

        mockMvc.perform(post("/api/v1/notes")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + otherToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40404));
    }

    // ---- getMyNoteDetail with tags ----

    @Test
    void getMyNoteDetail_withTags_returnsTags() throws Exception {
        AppUser user = createEnabledUser("note_detail_tag", "NoteDetailTag", "USER");
        String token = tokenOf(user);
        Long noteId = insertNote(user.getId(), "Note With Tags Detail", "# detail", "detail summary", 0, null);
        Long tagId1 = insertTag(user.getId(), "Java");
        Long tagId2 = insertTag(user.getId(), "Spring");

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                noteId, tagId1, now);
        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                noteId, tagId2, now);

        mockMvc.perform(get("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(noteId.intValue()))
                .andExpect(jsonPath("$.data.tags.length()").value(2))
                .andExpect(jsonPath("$.data.tags[0].id").value(tagId1.intValue()))
                .andExpect(jsonPath("$.data.tags[0].name").value("Java"))
                .andExpect(jsonPath("$.data.tags[1].id").value(tagId2.intValue()))
                .andExpect(jsonPath("$.data.tags[1].name").value("Spring"));
    }

    // ---- updateNote with tagIds ----

    @Test
    void updateNote_withValidTagIds_replacesTags() throws Exception {
        AppUser user = createEnabledUser("note_upd_tag", "NoteUpdTag", "USER");
        String token = tokenOf(user);
        Long noteId = insertNote(user.getId(), "Note For Tag Update", "content", "summary", 0, null);
        Long tagId1 = insertTag(user.getId(), "Java");
        Long tagId2 = insertTag(user.getId(), "Spring");

        String body = """
                {
                  "title": "Note For Tag Update",
                  "contentMd": "# updated",
                  "tagIds": [%d, %d]
                }
                """.formatted(tagId1, tagId2);

        mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.tags.length()").value(2))
                .andExpect(jsonPath("$.data.tags[0].id").value(tagId1.intValue()))
                .andExpect(jsonPath("$.data.tags[0].name").value("Java"))
                .andExpect(jsonPath("$.data.tags[1].id").value(tagId2.intValue()))
                .andExpect(jsonPath("$.data.tags[1].name").value("Spring"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM note_tag WHERE note_id = ?", Integer.class, noteId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void updateNote_withEmptyTagIds_clearsTags() throws Exception {
        AppUser user = createEnabledUser("note_upd_clear_tag", "NoteUpdClearTag", "USER");
        String token = tokenOf(user);
        Long noteId = insertNote(user.getId(), "Note To Clear Tags", "content", "summary", 0, null);
        Long tagId = insertTag(user.getId(), "To Be Cleared");

        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                noteId, tagId, LocalDateTime.now());

        String body = """
                {
                  "title": "Note To Clear Tags",
                  "contentMd": "# updated",
                  "tagIds": []
                }
                """;

        mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.tags.length()").value(0));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM note_tag WHERE note_id = ?", Integer.class, noteId);
        assertThat(count).isEqualTo(0);
    }

    // ---- listNotes with keyword ----

    @Test
    void listNotes_withKeyword_returnsMatchedOwnNotes() throws Exception {
        AppUser user = createEnabledUser("note_list_kw", "NoteListKw", "USER");
        String token = tokenOf(user);

        insertNote(user.getId(), "Spring Boot Guide", "content about DI", "spring summary", 0, null);
        insertNote(user.getId(), "Java Basics", "learn java spring", "java basics summary", 0, null);
        insertNote(user.getId(), "Docker Tutorial", "container stuff", "docker intro", 0, null);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&keyword=spring")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].title").value("Java Basics"))
                .andExpect(jsonPath("$.data.items[1].title").value("Spring Boot Guide"));
    }

    @Test
    void listNotes_withKeyword_doesNotReturnOtherUsersOrDeletedNotes() throws Exception {
        AppUser user = createEnabledUser("note_list_kw_owner", "NoteListKwOwner", "USER");
        AppUser other = createEnabledUser("note_list_kw_other", "NoteListKwOther", "USER");
        String token = tokenOf(user);

        insertNote(user.getId(), "My Spring Note", "my spring content", "my summary", 0, null);
        insertNote(other.getId(), "Other Spring Note", "other spring content", "other summary", 0, null);
        Long deletedId = insertNote(user.getId(), "Deleted Spring Note", "deleted spring content", "deleted summary", 0, null);
        jdbcTemplate.update("UPDATE note SET deleted = 1, deleted_at = ? WHERE id = ?",
                LocalDateTime.now(), deletedId);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&keyword=spring")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("My Spring Note"));
    }

    @Test
    void listNotes_withKeywordAndCategoryId_returnsIntersection() throws Exception {
        AppUser user = createEnabledUser("note_list_kw_cat", "NoteListKwCat", "USER");
        String token = tokenOf(user);
        Long catId = insertCategory(user.getId(), "Tech");

        insertNote(user.getId(), "Spring In Tech", "spring di content", "spring summary", 0, null, catId);
        insertNote(user.getId(), "Spring No Cat", "spring boot content", "spring summary", 0, null);
        insertNote(user.getId(), "Docker In Tech", "container content", "docker summary", 0, null, catId);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&keyword=spring&categoryId=" + catId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Spring In Tech"));
    }

    @Test
    void listNotes_withKeyword_percentSign_matchesLiteral() throws Exception {
        AppUser user = createEnabledUser("note_list_kw_pct", "NoteListKwPct", "USER");
        String token = tokenOf(user);

        insertNote(user.getId(), "100% Java", "percent content", "percent summary", 0, null);
        insertNote(user.getId(), "Plain Java", "plain content", "plain summary", 0, null);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&keyword=%")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("100% Java"));
    }

    @Test
    void listNotes_withKeyword_underscore_matchesLiteral() throws Exception {
        AppUser user = createEnabledUser("note_list_kw_usc", "NoteListKwUsc", "USER");
        String token = tokenOf(user);

        insertNote(user.getId(), "under_score", "underscore content", "underscore summary", 0, null);
        insertNote(user.getId(), "underscore", "no underscore in title", "underscore summary", 0, null);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&keyword=_")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("under_score"));
    }

    @Test
    void listNotes_withTooLongKeyword_returns40001() throws Exception {
        AppUser user = createEnabledUser("note_list_kw_long", "NoteListKwLong", "USER");
        String token = tokenOf(user);

        String longKeyword = "a".repeat(101);

        mockMvc.perform(get("/api/v1/notes?page=1&size=20&keyword=" + longKeyword)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void updateNote_withTooManyTagIds_returns400() throws Exception {
        AppUser user = createEnabledUser("note_upd_tags_max", "NoteUpdTagsMax", "USER");
        String token = tokenOf(user);
        Long noteId = insertNote(user.getId(), "Note Max Tags", "content", "summary", 0, null);

        String body = """
                {
                  "title": "Note Max Tags",
                  "contentMd": "# updated",
                  "tagIds": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                }
                """;

        mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }
}
