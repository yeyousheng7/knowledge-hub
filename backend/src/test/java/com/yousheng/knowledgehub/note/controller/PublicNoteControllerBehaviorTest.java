package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicNoteControllerBehaviorTest extends AbstractControllerBehaviorTest {

    @Test
    void listPublicNotes_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void listPublicNotes_returnsOnlyPublicNormalNotDeleted() throws Exception {
        AppUser user = createEnabledUser("public_list_filter", "PublicListFilter", "USER");

        Long publicNormal = insertNote(user.getId(), "Public Normal", "content", "summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 10, 0),
                publicNormal
        );

        insertNote(user.getId(), "Private Normal", "content", "summary", 0, null);
        Long publicTakenDown = insertNote(user.getId(), "Public Taken Down", "content", "summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'TAKEN_DOWN', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 10, 5),
                publicTakenDown
        );

        Long publicDeleted = createDeletedNote(user.getId(), "Public Deleted", "content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 10, 10),
                publicDeleted
        );

        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Public Normal"));
    }

    @Test
    void listPublicNotes_doesNotReturnPrivateNotes() throws Exception {
        AppUser user = createEnabledUser("public_list_private", "PublicListPrivate", "USER");

        insertNote(user.getId(), "Private Note", "private content", "private summary", 0, null);

        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void listPublicNotes_doesNotReturnDeletedNotes() throws Exception {
        AppUser user = createEnabledUser("public_list_deleted", "PublicListDeleted", "USER");

        Long noteId = createDeletedNote(user.getId(), "Deleted Public Note", "content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 11, 0),
                noteId
        );

        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void listPublicNotes_ordersByPublishedAtDescThenIdDesc() throws Exception {
        AppUser user = createEnabledUser("public_list_order", "PublicListOrder", "USER");
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 3, 13, 0);

        Long lowPublished = insertNote(
                user.getId(),
                "Order Low Published",
                "content",
                "summary",
                LocalDateTime.of(2026, 6, 3, 12, 0),
                updatedAt,
                0,
                null
        );

        Long tiePublishedLowerId = insertNote(
                user.getId(),
                "Order Tie Lower Id",
                "content",
                "summary",
                LocalDateTime.of(2026, 6, 3, 12, 10),
                updatedAt,
                0,
                null
        );

        Long tiePublishedHigherId = insertNote(
                user.getId(),
                "Order Tie Higher Id",
                "content",
                "summary",
                LocalDateTime.of(2026, 6, 3, 12, 20),
                updatedAt,
                0,
                null
        );

        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 13, 10),
                lowPublished
        );
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 13, 20),
                tiePublishedLowerId
        );
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 13, 20),
                tiePublishedHigherId
        );

        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].id").value(tiePublishedHigherId.intValue()))
                .andExpect(jsonPath("$.data.items[1].id").value(tiePublishedLowerId.intValue()))
                .andExpect(jsonPath("$.data.items[2].id").value(lowPublished.intValue()));
    }

    @Test
    void getPublicNoteDetail_publicNote_returns200() throws Exception {
        AppUser user = createEnabledUser("public_detail_owner", "PublicDetailOwner", "USER");

        Long noteId = insertNote(user.getId(), "Public Detail Note", "# public detail", "detail summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 11, 30),
                noteId
        );

        mockMvc.perform(get("/api/v1/public/notes/{noteId}", noteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(noteId.intValue()))
                .andExpect(jsonPath("$.data.title").value("Public Detail Note"));
    }

    @Test
    void listPublicNotes_returnsTags() throws Exception {
        AppUser user = createEnabledUser("public_list_tags", "PublicListTags", "USER");

        Long noteId = insertNote(user.getId(), "Public With Tags", "content", "summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 10, 0),
                noteId
        );

        Long tagId1 = insertTag(user.getId(), "Java");
        Long tagId2 = insertTag(user.getId(), "Spring");

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                noteId, tagId1, now);
        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                noteId, tagId2, now);

        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].tags.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].tags[0].name").value("Java"))
                .andExpect(jsonPath("$.data.items[0].tags[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].tags[1].name").value("Spring"))
                .andExpect(jsonPath("$.data.items[0].tags[1].id").doesNotExist());
    }

    @Test
    void getPublicNoteDetail_returnsTags() throws Exception {
        AppUser user = createEnabledUser("public_detail_tags", "PublicDetailTags", "USER");

        Long noteId = insertNote(user.getId(), "Public Detail With Tags", "# detail", "summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 11, 30),
                noteId
        );

        Long tagId = insertTag(user.getId(), "Java");
        jdbcTemplate.update(
                "INSERT INTO note_tag (note_id, tag_id, created_at) VALUES (?, ?, ?)",
                noteId, tagId, LocalDateTime.now());

        mockMvc.perform(get("/api/v1/public/notes/{noteId}", noteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.tags.length()").value(1))
                .andExpect(jsonPath("$.data.tags[0].name").value("Java"))
                .andExpect(jsonPath("$.data.tags[0].id").doesNotExist());
    }

    @Test
    void listPublicNotes_returnsAuthor() throws Exception {
        AppUser user = createEnabledUser("public_author", "Public Author", "USER");

        Long noteId = insertNote(user.getId(), "Public With Author", "content", "summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 9, 0),
                noteId
        );

        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].author.username").value("public_author"))
                .andExpect(jsonPath("$.data.items[0].author.nickname").value("Public Author"))
                .andExpect(jsonPath("$.data.items[0].author.id").doesNotExist());
    }

    @Test
    void getPublicNoteDetail_returnsAuthor() throws Exception {
        AppUser user = createEnabledUser("public_detail_author", "Public Detail Author", "USER");

        Long noteId = insertNote(user.getId(), "Public Detail With Author", "# detail", "summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 9, 30),
                noteId
        );

        mockMvc.perform(get("/api/v1/public/notes/{noteId}", noteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.author.username").value("public_detail_author"))
                .andExpect(jsonPath("$.data.author.nickname").value("Public Detail Author"))
                .andExpect(jsonPath("$.data.author.id").doesNotExist());
    }

    @Test
    void getPublicNoteDetail_privateDeletedTakenDown_returns40401() throws Exception {
        AppUser user = createEnabledUser("public_detail_filter", "PublicDetailFilter", "USER");

        Long privateNoteId = insertNote(user.getId(), "Private Detail", "content", "summary", 0, null);

        Long deletedNoteId = createDeletedNote(user.getId(), "Deleted Detail", "content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 12, 0),
                deletedNoteId
        );

        Long takenDownNoteId = insertNote(user.getId(), "Taken Down Detail", "content", "summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'TAKEN_DOWN', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 12, 5),
                takenDownNoteId
        );

        mockMvc.perform(get("/api/v1/public/notes/{noteId}", privateNoteId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        mockMvc.perform(get("/api/v1/public/notes/{noteId}", deletedNoteId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        mockMvc.perform(get("/api/v1/public/notes/{noteId}", takenDownNoteId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    // ---- listPublicNotes with keyword ----

    @Test
    void public_listWithKeyword_matchesTitleSummaryContent() throws Exception {
        AppUser user = createEnabledUser("pub_list_kw", "PubListKw", "USER");

        // keyword in title
        Long kwTitle = insertNote(user.getId(), "Spring Boot Guide", "content about DI", "no match here", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 14, 0), kwTitle);

        // keyword in summary
        Long kwSummary = insertNote(user.getId(), "Java Basics", "learn java", "spring summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 14, 1), kwSummary);

        // keyword in content (case-insensitive: keyword=spring matches "SPRING")
        Long kwContent = insertNote(user.getId(), "Docker Tutorial", "learn SPRING framework", "docker intro", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 14, 2), kwContent);

        // no keyword
        Long noKw = insertNote(user.getId(), "Other", "other content", "other summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 14, 3), noKw);

        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20&keyword=spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(3));
    }

    @Test
    void public_listWithKeyword_doesNotReturnPrivateDeletedTakenDownUnpublished() throws Exception {
        AppUser user = createEnabledUser("pub_list_kw_filter", "PubListKwFilter", "USER");

        // public + normal + published + matches keyword
        Long match = insertNote(user.getId(), "Spring Public", "spring content", "spring summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 15, 0), match);

        // private + matches keyword
        insertNote(user.getId(), "Spring Private", "spring content", "spring summary", 0, null);

        // deleted + matches keyword
        Long deleted = createDeletedNote(user.getId(), "Spring Deleted", "spring content", "spring summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 15, 5), deleted);

        // taken down + matches keyword
        Long takenDown = insertNote(user.getId(), "Spring Taken Down", "spring content", "spring summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'TAKEN_DOWN', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 15, 10), takenDown);

        // public + normal + published_at IS NULL (unpublished) + matches keyword
        Long unpublished = insertNote(user.getId(), "Spring Unpublished", "spring content", "spring summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL' WHERE id = ?",
                unpublished);

        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20&keyword=spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Spring Public"));
    }

    @Test
    void public_listWithEscapedKeyword_matchesLiteralPercentUnderscoreBang() throws Exception {
        AppUser user = createEnabledUser("pub_list_kw_esc", "PubListKwEsc", "USER");

        // note with literal %
        Long percentNote = insertNote(user.getId(), "100% Pure", "percent content", "percent summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 16, 0), percentNote);

        // note without % as wildcard trap (LIKE '%%' would match everything)
        Long noPercent = insertNote(user.getId(), "No Percent", "no percent content", "no percent summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 16, 1), noPercent);

        // note with literal _
        Long underscoreNote = insertNote(user.getId(), "under_score", "underscore content", "underscore summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 16, 2), underscoreNote);

        // note without _ as wildcard trap (LIKE '_' would match any single char)
        Long noUnderscore = insertNote(user.getId(), "X", "underscore content", "underscore summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 16, 3), noUnderscore);

        // note with literal !
        Long bangNote = insertNote(user.getId(), "Important!", "bang content", "bang summary", 0, null);
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 3, 16, 4), bangNote);

        // % matches literal percent only
        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20&keyword=%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("100% Pure"));

        // _ matches literal underscore only
        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20&keyword=_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("under_score"));

        // ! matches literal exclamation
        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20&keyword=!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Important!"));
    }

    @Test
    void public_listWithKeywordTooLong_returns40001() throws Exception {
        String longKeyword = "a".repeat(101);

        mockMvc.perform(get("/api/v1/public/notes?page=1&size=20&keyword=" + longKeyword))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }
}
