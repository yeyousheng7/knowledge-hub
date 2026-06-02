package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.security.JwtConstants;
import com.yousheng.knowledgehub.security.JwtTokenProvider;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NoteControllerBehaviorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM note");
        jdbcTemplate.execute("DELETE FROM app_user");
    }

    @Test
    void createNote_withTokenAndTitle_returns200() throws Exception {
        AppUser user = createEnabledUser("note_user_1", "NoteUser", "USER");
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

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
    void listNotes_withToken_returnsOwnActiveNotesInOrder() throws Exception {
        AppUser owner = createEnabledUser("note_list_owner", "NoteListOwner", "USER");
        AppUser otherUser = createEnabledUser("note_list_other", "NoteListOther", "USER");
        String token = jwtTokenProvider.generateAccessToken(owner.getId(), owner.getUsername(), owner.getRole()).accessToken();

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
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

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
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

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
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

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
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

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
        String token = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        ).accessToken();

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
    void getMyNoteDetail_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notes/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyNoteDetail_ownNote_returns200() throws Exception {
        AppUser user = createEnabledUser("note_detail_owner", "NoteDetailOwner", "USER");
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

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
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

        mockMvc.perform(get("/api/v1/notes/{noteId}", 999_999L)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void getMyNoteDetail_otherUsersNote_returns40401() throws Exception {
        AppUser owner = createEnabledUser("note_detail_owner_2", "NoteDetailOwner2", "USER");
        AppUser viewer = createEnabledUser("note_detail_viewer", "NoteDetailViewer", "USER");
        String viewerToken = jwtTokenProvider.generateAccessToken(viewer.getId(), viewer.getUsername(), viewer.getRole()).accessToken();

        Long noteId = insertNote(owner.getId(), "Other User Note", "other content", "other summary", 0, null);

        mockMvc.perform(get("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + viewerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void getMyNoteDetail_deletedNote_returns40401() throws Exception {
        AppUser user = createEnabledUser("note_detail_deleted", "NoteDetailDeleted", "USER");
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

        Long noteId = insertNote(user.getId(), "Deleted Note", "deleted content", "deleted summary", 1, LocalDateTime.now());

        mockMvc.perform(get("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void getMyNoteDetail_disabledUserWithValidToken_returns40301() throws Exception {
        AppUser user = createDisabledUser("note_detail_disabled", "NoteDetailDisabled", "USER");
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

        Long noteId = insertNote(user.getId(), "Disabled User Note", "disabled content", "disabled summary", 0, null);

        mockMvc.perform(get("/api/v1/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void updateNote_withToken_updatesOwnNote_returns200() throws Exception {
        AppUser user = createEnabledUser("note_update_owner", "NoteUpdateOwner", "USER");
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

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
        String viewerToken = jwtTokenProvider.generateAccessToken(viewer.getId(), viewer.getUsername(), viewer.getRole()).accessToken();

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
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

        Long noteId = insertNote(user.getId(), "Deleted Note", "deleted content", "deleted summary", 1, LocalDateTime.now());

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
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

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
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

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
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

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

    private AppUser createEnabledUser(String username, String nickname, String role) {
        LocalDateTime now = LocalDateTime.now();

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash("unused-hash");
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(UserStatus.ENABLED.name());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        appUserMapper.insert(user);
        return user;
    }

    private AppUser createDisabledUser(String username, String nickname, String role) {
        AppUser user = createEnabledUser(username, nickname, role);
        user.setStatus(UserStatus.DISABLED.name());
        appUserMapper.updateById(user);
        return user;
    }

    private Long insertNote(
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

    private Long insertNote(
            Long userId,
            String title,
            String contentMd,
            String summary,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int deleted,
            LocalDateTime deletedAt
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO note (
                                user_id, title, content_md, summary, visibility,
                                created_at, updated_at, published_at, moderation_status,
                                moderated_at, deleted, deleted_at
                        ) VALUES (?, ?, ?, ?, 'PRIVATE', ?, ?, NULL, 'NORMAL', NULL, ?, ?)
                        """,
                userId,
                title,
                contentMd,
                summary,
                createdAt,
                updatedAt,
                deleted,
                deletedAt
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}