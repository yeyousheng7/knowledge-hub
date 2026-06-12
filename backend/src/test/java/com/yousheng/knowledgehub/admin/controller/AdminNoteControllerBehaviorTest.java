package com.yousheng.knowledgehub.admin.controller;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminNoteControllerBehaviorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM note_tag");
        jdbcTemplate.execute("DELETE FROM note");
        jdbcTemplate.execute("DELETE FROM tag");
        jdbcTemplate.execute("DELETE FROM category");
        jdbcTemplate.execute("DELETE FROM app_user");
    }

    @Test
    void admin_takeDownPublicNote_returns200() throws Exception {
        AppUser admin = createEnabledUser("admin_takedown", "AdminTakedown", "ADMIN");
        AppUser user = createEnabledUser("note_owner_td", "NoteOwnerTd", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Public Note", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.noteId").value(noteId.intValue()))
                .andExpect(jsonPath("$.data.moderationStatus").value("TAKEN_DOWN"))
                .andExpect(jsonPath("$.data.moderatedAt").exists());

        String moderationStatus = jdbcTemplate.queryForObject(
                "SELECT moderation_status FROM note WHERE id = ?", String.class, noteId);
        LocalDateTime moderatedAt = jdbcTemplate.queryForObject(
                "SELECT moderated_at FROM note WHERE id = ?", LocalDateTime.class, noteId);

        assertEquals("TAKEN_DOWN", moderationStatus);
        assertNotNull(moderatedAt);
    }

    @Test
    void user_takeDownPublicNote_returns403() throws Exception {
        AppUser user = createEnabledUser("user_takedown", "UserTakedown", "USER");
        String userToken = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Public Note", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_takeDownPublicNote_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void takeDownPublicNote_thenPublicDetailReturns404() throws Exception {
        AppUser admin = createEnabledUser("admin_detail_404", "AdminDetail404", "ADMIN");
        AppUser user = createEnabledUser("note_owner_detail", "NoteOwnerDetail", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Public Note Detail", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/public/notes/{noteId}", noteId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void takeDownPublicNote_twice_isIdempotent() throws Exception {
        AppUser admin = createEnabledUser("admin_idempotent", "AdminIdempotent", "ADMIN");
        AppUser user = createEnabledUser("note_owner_idem", "NoteOwnerIdem", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Public Note Idem", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        LocalDateTime firstModeratedAt = jdbcTemplate.queryForObject(
                "SELECT moderated_at FROM note WHERE id = ?", LocalDateTime.class, noteId);

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.moderationStatus").value("TAKEN_DOWN"));

        String moderationStatus = jdbcTemplate.queryForObject(
                "SELECT moderation_status FROM note WHERE id = ?", String.class, noteId);
        LocalDateTime moderatedAt = jdbcTemplate.queryForObject(
                "SELECT moderated_at FROM note WHERE id = ?", LocalDateTime.class, noteId);

        assertEquals("TAKEN_DOWN", moderationStatus);
        assertEquals(firstModeratedAt, moderatedAt);
    }

    @Test
    void admin_takeDownNonExistentNote_returns40401() throws Exception {
        AppUser admin = createEnabledUser("admin_nonexist", "AdminNonexist", "ADMIN");
        String adminToken = tokenOf(admin);

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", 999_999L)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void admin_takeDownPrivateNote_returns40401() throws Exception {
        AppUser admin = createEnabledUser("admin_private", "AdminPrivate", "ADMIN");
        AppUser user = createEnabledUser("note_owner_private", "NoteOwnerPrivate", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Private Note", "# content", "summary");

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void admin_takeDownDeletedNote_returns40401() throws Exception {
        AppUser admin = createEnabledUser("admin_deleted", "AdminDeleted", "ADMIN");
        AppUser user = createEnabledUser("note_owner_del", "NoteOwnerDel", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Deleted Public Note", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ?, deleted = 1, deleted_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.now(),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void takeDownPublicNote_thenDelete_thenTakeDownAgain_returns40401() throws Exception {
        AppUser admin = createEnabledUser("admin_del_after", "AdminDelAfter", "ADMIN");
        AppUser user = createEnabledUser("note_owner_del2", "NoteOwnerDel2", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Public Then Deleted", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        jdbcTemplate.update("UPDATE note SET deleted = 1, deleted_at = ? WHERE id = ?",
                LocalDateTime.now(), noteId);

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void takeDownPublicNote_thenMakePrivate_thenTakeDownAgain_returns40401() throws Exception {
        AppUser admin = createEnabledUser("admin_priv_after", "AdminPrivAfter", "ADMIN");
        AppUser user = createEnabledUser("note_owner_priv2", "NoteOwnerPriv2", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Public Then Private", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        jdbcTemplate.update("UPDATE note SET visibility = 'PRIVATE' WHERE id = ?", noteId);

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    // ---- restore ----

    @Test
    void admin_restoreTakenDownPublicNote_returns200() throws Exception {
        AppUser admin = createEnabledUser("admin_restore", "AdminRestore", "ADMIN");
        AppUser user = createEnabledUser("note_owner_rest", "NoteOwnerRest", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Public Note Restore", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/restore", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.noteId").value(noteId.intValue()))
                .andExpect(jsonPath("$.data.moderationStatus").value("NORMAL"))
                .andExpect(jsonPath("$.data.moderatedAt").exists());

        String moderationStatus = jdbcTemplate.queryForObject(
                "SELECT moderation_status FROM note WHERE id = ?", String.class, noteId);

        assertEquals("NORMAL", moderationStatus);
    }

    @Test
    void restoreTakenDownPublicNote_thenPublicDetailReturns200() throws Exception {
        AppUser admin = createEnabledUser("admin_rest_detail", "AdminRestDetail", "ADMIN");
        AppUser user = createEnabledUser("note_owner_rd", "NoteOwnerRd", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Public Note Rest Detail", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/restore", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/public/notes/{noteId}", noteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(noteId.intValue()));
    }

    @Test
    void user_restorePublicNote_returns403() throws Exception {
        AppUser user = createEnabledUser("user_restore", "UserRestore", "USER");
        String userToken = tokenOf(user);

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/restore", 1L)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_restorePublicNote_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/restore", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restorePublicNote_twice_isIdempotent() throws Exception {
        AppUser admin = createEnabledUser("admin_rest_idem", "AdminRestIdem", "ADMIN");
        AppUser user = createEnabledUser("note_owner_ri", "NoteOwnerRi", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Public Note Rest Idem", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/restore", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        LocalDateTime firstModeratedAt = jdbcTemplate.queryForObject(
                "SELECT moderated_at FROM note WHERE id = ?", LocalDateTime.class, noteId);

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/restore", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.moderationStatus").value("NORMAL"));

        String moderationStatus = jdbcTemplate.queryForObject(
                "SELECT moderation_status FROM note WHERE id = ?", String.class, noteId);
        LocalDateTime moderatedAt = jdbcTemplate.queryForObject(
                "SELECT moderated_at FROM note WHERE id = ?", LocalDateTime.class, noteId);

        assertEquals("NORMAL", moderationStatus);
        assertEquals(firstModeratedAt, moderatedAt);
    }

    @Test
    void admin_restoreNonExistentNote_returns40401() throws Exception {
        AppUser admin = createEnabledUser("admin_rest_nx", "AdminRestNx", "ADMIN");
        String adminToken = tokenOf(admin);

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/restore", 999_999L)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void admin_restoreDeletedNote_returns40401() throws Exception {
        AppUser admin = createEnabledUser("admin_rest_del", "AdminRestDel", "ADMIN");
        AppUser user = createEnabledUser("note_owner_rd2", "NoteOwnerRd2", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Deleted Public Note", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        jdbcTemplate.update("UPDATE note SET deleted = 1, deleted_at = ? WHERE id = ?",
                LocalDateTime.now(), noteId);

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/restore", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void admin_restorePrivateNote_returns40401() throws Exception {
        AppUser admin = createEnabledUser("admin_rest_priv", "AdminRestPriv", "ADMIN");
        AppUser user = createEnabledUser("note_owner_rp", "NoteOwnerRp", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Public Then Private", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        jdbcTemplate.update("UPDATE note SET visibility = 'PRIVATE' WHERE id = ?", noteId);

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/restore", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void restoreNormalNote_isIdempotent() throws Exception {
        AppUser admin = createEnabledUser("admin_rest_norm", "AdminRestNorm", "ADMIN");
        AppUser user = createEnabledUser("note_owner_rn", "NoteOwnerRn", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Normal Public Note", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/restore", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.moderationStatus").value("NORMAL"));

        String moderationStatus = jdbcTemplate.queryForObject(
                "SELECT moderation_status FROM note WHERE id = ?", String.class, noteId);

        assertEquals("NORMAL", moderationStatus);
    }

    @Test
    void disabledAdmin_takeDownPublicNote_returns40301() throws Exception {
        AppUser disabledAdmin = createDisabledUser("disabled_admin_td", "DisabledAdminTd", "ADMIN");
        AppUser user = createEnabledUser("note_owner_da", "NoteOwnerDa", "USER");
        String adminToken = tokenOf(disabledAdmin);

        Long noteId = insertNote(user.getId(), "Public Note", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/take-down", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void disabledAdmin_restorePublicNote_returns40301() throws Exception {
        AppUser disabledAdmin = createDisabledUser("disabled_admin_rest", "DisabledAdminRest", "ADMIN");
        String adminToken = tokenOf(disabledAdmin);

        mockMvc.perform(post("/api/v1/admin/notes/{noteId}/restore", 1L)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    // ---- detail ----

    @Test
    void admin_getPublicNoteDetail_returnsDetail() throws Exception {
        AppUser admin = createEnabledUser("admin_detail", "AdminDetail", "ADMIN");
        AppUser user = createEnabledUser("note_owner_dtl", "NoteOwnerDtl", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Public Note Detail", "# content", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                noteId
        );

        mockMvc.perform(get("/api/v1/admin/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.noteId").value(noteId.intValue()))
                .andExpect(jsonPath("$.data.title").value("Public Note Detail"))
                .andExpect(jsonPath("$.data.contentMd").value("# content"))
                .andExpect(jsonPath("$.data.summary").value("summary"))
                .andExpect(jsonPath("$.data.author.userId").value(user.getId().intValue()))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.moderationStatus").value("NORMAL"));
    }

    @Test
    void admin_getTakenDownPublicNoteDetail_returnsDetail() throws Exception {
        AppUser admin = createEnabledUser("admin_detail_td", "AdminDetailTd", "ADMIN");
        AppUser user = createEnabledUser("note_owner_dtl_td", "NoteOwnerDtlTd", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Taken Down Note", "# taken", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'TAKEN_DOWN', published_at = ?, moderated_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.of(2026, 6, 11, 10, 0),
                noteId
        );

        mockMvc.perform(get("/api/v1/admin/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.noteId").value(noteId.intValue()))
                .andExpect(jsonPath("$.data.moderationStatus").value("TAKEN_DOWN"));
    }

    @Test
    void admin_getPrivateNoteDetail_returns40401() throws Exception {
        AppUser admin = createEnabledUser("admin_detail_priv", "AdminDetailPriv", "ADMIN");
        AppUser user = createEnabledUser("note_owner_dp", "NoteOwnerDp", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Private Note", "# private", "summary");

        mockMvc.perform(get("/api/v1/admin/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void admin_getDeletedNoteDetail_returns40401() throws Exception {
        AppUser admin = createEnabledUser("admin_detail_del", "AdminDetailDel", "ADMIN");
        AppUser user = createEnabledUser("note_owner_dd", "NoteOwnerDd", "USER");
        String adminToken = tokenOf(admin);

        Long noteId = insertNote(user.getId(), "Deleted Note", "# deleted", "summary");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ?, deleted = 1, deleted_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.now(),
                noteId
        );

        mockMvc.perform(get("/api/v1/admin/notes/{noteId}", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void user_getAdminNoteDetail_returns403() throws Exception {
        AppUser user = createEnabledUser("user_detail", "UserDetail", "USER");
        String userToken = tokenOf(user);

        mockMvc.perform(get("/api/v1/admin/notes/{noteId}", 1L)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_getAdminNoteDetail_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notes/{noteId}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAdmin_getAdminNoteDetail_returns40301() throws Exception {
        AppUser disabledAdmin = createDisabledUser("disabled_admin_dtl", "DisabledAdminDtl", "ADMIN");
        String adminToken = tokenOf(disabledAdmin);

        mockMvc.perform(get("/api/v1/admin/notes/{noteId}", 1L)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    // ---- list ----

    @Test
    void admin_listPublicNotes_returnsPagedNotes() throws Exception {
        AppUser admin = createEnabledUser("admin_list", "AdminList", "ADMIN");
        AppUser user = createEnabledUser("note_owner_list", "NoteOwnerList", "USER");
        String adminToken = tokenOf(admin);

        Long note1 = insertNote(user.getId(), "Note One", "# one", "summary one");
        Long note2 = insertNote(user.getId(), "Note Two", "# two", "summary two");
        Long note3 = insertNote(user.getId(), "Note Three", "# three", "summary three");

        for (Long noteId : List.of(note1, note2, note3)) {
            jdbcTemplate.update(
                    "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                    LocalDateTime.of(2026, 6, 10, 10, 0), noteId);
        }

        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=2&keyword=&moderationStatus=")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void admin_listPublicNotes_includesTakenDownNotes() throws Exception {
        AppUser admin = createEnabledUser("admin_list_td", "AdminListTd", "ADMIN");
        AppUser user = createEnabledUser("note_owner_lt", "NoteOwnerLt", "USER");
        String adminToken = tokenOf(admin);

        Long normalId = insertNote(user.getId(), "Normal Note", "# normal", "summary normal");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0), normalId);

        Long takenDownId = insertNote(user.getId(), "Taken Down Note", "# taken", "summary taken");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'TAKEN_DOWN', published_at = ?, moderated_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 5), LocalDateTime.of(2026, 6, 11, 10, 0), takenDownId);

        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=20&keyword=&moderationStatus=")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void admin_listPublicNotes_withModerationStatus_returnsFilteredNotes() throws Exception {
        AppUser admin = createEnabledUser("admin_list_filt", "AdminListFilt", "ADMIN");
        AppUser user = createEnabledUser("note_owner_lf", "NoteOwnerLf", "USER");
        String adminToken = tokenOf(admin);

        Long normalId = insertNote(user.getId(), "Normal Note", "# normal", "summary normal");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0), normalId);

        Long takenDownId = insertNote(user.getId(), "Taken Down Note", "# taken", "summary taken");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'TAKEN_DOWN', published_at = ?, moderated_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 5), LocalDateTime.of(2026, 6, 11, 10, 0), takenDownId);

        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=20&keyword=&moderationStatus=TAKEN_DOWN")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].moderationStatus").value("TAKEN_DOWN"));
    }

    @Test
    void admin_listPublicNotes_withKeyword_returnsMatchedNotes() throws Exception {
        AppUser admin = createEnabledUser("admin_list_kw", "AdminListKw", "ADMIN");
        AppUser user = createEnabledUser("note_owner_lkw", "NoteOwnerLkw", "USER");
        String adminToken = tokenOf(admin);

        Long note1 = insertNote(user.getId(), "Spring Boot Guide", "# spring", "boot summary");
        Long note2 = insertNote(user.getId(), "Java Basics", "# java", "learn java");
        Long note3 = insertNote(user.getId(), "Docker Tutorial", "# docker", "container stuff");

        for (Long noteId : List.of(note1, note2, note3)) {
            jdbcTemplate.update(
                    "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                    LocalDateTime.of(2026, 6, 10, 10, 0), noteId);
        }

        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=20&keyword=spring&moderationStatus=")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Spring Boot Guide"));
    }

    @Test
    void admin_listPublicNotes_doesNotReturnPrivateOrDeletedNotes() throws Exception {
        AppUser admin = createEnabledUser("admin_list_filter", "AdminListFilter", "ADMIN");
        AppUser user = createEnabledUser("note_owner_filt", "NoteOwnerFilt", "USER");
        String adminToken = tokenOf(admin);

        Long publicId = insertNote(user.getId(), "Public Note", "# public", "summary public");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0), publicId);

        Long privateId = insertNote(user.getId(), "Private Note", "# private", "summary private");

        Long deletedId = insertNote(user.getId(), "Deleted Note", "# deleted", "summary deleted");
        jdbcTemplate.update(
                "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ?, deleted = 1, deleted_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 6, 10, 10, 0), LocalDateTime.now(), deletedId);

        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=20&keyword=&moderationStatus=")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Public Note"));
    }

    @Test
    void user_listAdminNotes_returns403() throws Exception {
        AppUser user = createEnabledUser("user_list_notes", "UserListNotes", "USER");
        String userToken = tokenOf(user);

        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=20&keyword=&moderationStatus=")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_listAdminNotes_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=20&keyword=&moderationStatus="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAdmin_listAdminNotes_returns40301() throws Exception {
        AppUser disabledAdmin = createDisabledUser("disabled_admin_list_n", "DisabledAdminListN", "ADMIN");
        String adminToken = tokenOf(disabledAdmin);

        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=20&keyword=&moderationStatus=")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void admin_listPublicNotes_withoutOptionalParams_returns200() throws Exception {
        AppUser admin = createEnabledUser("admin_list_opt", "AdminListOpt", "ADMIN");
        String adminToken = tokenOf(admin);

        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=20")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void admin_listPublicNotes_ordersByUpdatedAtDescThenIdDesc() throws Exception {
        AppUser admin = createEnabledUser("admin_list_order", "AdminListOrder", "ADMIN");
        AppUser user = createEnabledUser("note_owner_lo", "NoteOwnerLo", "USER");
        String adminToken = tokenOf(admin);

        Long oldestId = insertNote(user.getId(), "Oldest", "# oldest", "summary");
        Long middleId = insertNote(user.getId(), "Middle", "# middle", "summary");
        Long newestId = insertNote(user.getId(), "Newest", "# newest", "summary");

        LocalDateTime base = LocalDateTime.of(2026, 6, 10, 10, 0);
        for (Long noteId : List.of(oldestId, middleId, newestId)) {
            jdbcTemplate.update(
                    "UPDATE note SET visibility = 'PUBLIC', moderation_status = 'NORMAL', published_at = ? WHERE id = ?",
                    base, noteId);
        }
        jdbcTemplate.update("UPDATE note SET updated_at = ? WHERE id = ?", base.plusMinutes(10), oldestId);
        jdbcTemplate.update("UPDATE note SET updated_at = ? WHERE id = ?", base.plusMinutes(20), middleId);
        jdbcTemplate.update("UPDATE note SET updated_at = ? WHERE id = ?", base.plusMinutes(30), newestId);

        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=20")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items[0].title").value("Newest"))
                .andExpect(jsonPath("$.data.items[1].title").value("Middle"))
                .andExpect(jsonPath("$.data.items[2].title").value("Oldest"));
    }

    @Test
    void admin_listPublicNotes_pageZero_returns40001() throws Exception {
        AppUser admin = createEnabledUser("admin_list_p0", "AdminListP0", "ADMIN");
        String adminToken = tokenOf(admin);

        mockMvc.perform(get("/api/v1/admin/notes?page=0&size=20")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void admin_listPublicNotes_sizeExceedsMax_returns40001() throws Exception {
        AppUser admin = createEnabledUser("admin_list_smax", "AdminListSmax", "ADMIN");
        String adminToken = tokenOf(admin);

        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=101")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void admin_listPublicNotes_keywordTooLong_returns40001() throws Exception {
        AppUser admin = createEnabledUser("admin_list_kwl", "AdminListKwl", "ADMIN");
        String adminToken = tokenOf(admin);

        String longKeyword = "a".repeat(101);

        mockMvc.perform(get("/api/v1/admin/notes?page=1&size=20&keyword=" + longKeyword)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    // ---- helpers ----

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

    private String tokenOf(AppUser user) {
        return jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        ).accessToken();
    }

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
}
