package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.security.JwtConstants;
import com.yousheng.knowledgehub.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotePublishControllerBehaviorTest extends AbstractControllerBehaviorTest {

    @Test
    void publishNote_withToken_publishesOwnPrivateNote_returns200() throws Exception {
        AppUser user = createEnabledUser("note_publish_owner", "NotePublishOwner", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Private Note", "private content", "private summary", 0, null);

        mockMvc.perform(post("/api/v1/notes/{noteId}/publish", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(noteId.intValue()))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.publishedAt").exists());

        String visibility = jdbcTemplate.queryForObject("SELECT visibility FROM note WHERE id = ?", String.class, noteId);
        LocalDateTime publishedAt = jdbcTemplate.queryForObject("SELECT published_at FROM note WHERE id = ?", LocalDateTime.class, noteId);

        assertThat(visibility).isEqualTo("PUBLIC");
        assertThat(publishedAt).isNotNull();
    }

    @Test
    void publishNote_withoutToken_returns401() throws Exception {
        Long noteId = insertNote(createEnabledUser("note_publish_unauth", "NotePublishUnauth", "USER").getId(), "Private Note", "private content", "private summary", 0, null);

        mockMvc.perform(post("/api/v1/notes/{noteId}/publish", noteId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publishNote_otherUsersNote_returns40401() throws Exception {
        AppUser owner = createEnabledUser("note_publish_owner_2", "NotePublishOwner2", "USER");
        AppUser viewer = createEnabledUser("note_publish_viewer", "NotePublishViewer", "USER");
        String viewerToken = tokenOf(viewer);

        Long noteId = insertNote(owner.getId(), "Owner Private Note", "owner content", "owner summary", 0, null);

        mockMvc.perform(post("/api/v1/notes/{noteId}/publish", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + viewerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void publishNote_deletedNote_returns40401() throws Exception {
        AppUser user = createEnabledUser("note_publish_deleted", "NotePublishDeleted", "USER");
        String token = tokenOf(user);

        Long noteId = createDeletedNote(user.getId(), "Deleted Note", "deleted content", "deleted summary");

        mockMvc.perform(post("/api/v1/notes/{noteId}/publish", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void publishNote_withDisabledUser_returns40301() throws Exception {
        AppUser user = createDisabledUser("note_publish_disabled", "NotePublishDisabled", "USER");
        String token = tokenOf(user);

        Long noteId = insertNote(user.getId(), "Disabled Private Note", "disabled content", "disabled summary", 0, null);

        mockMvc.perform(post("/api/v1/notes/{noteId}/publish", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void unpublishNote_withToken_unpublishesOwnPublicNote_returns200() throws Exception {
        AppUser user = createEnabledUser("note_unpublish_owner", "NoteUnpublishOwner", "USER");
        String token = tokenOf(user);

        LocalDateTime originalPublishedAt = LocalDateTime.of(2026, 1, 2, 10, 0);
        Long noteId = insertNote(user.getId(), "Public Note", "public content", "public summary", LocalDateTime.of(2026, 1, 2, 9, 0), LocalDateTime.of(2026, 1, 2, 9, 30), 0, null);
        jdbcTemplate.update("UPDATE note SET visibility = 'PUBLIC', published_at = ? WHERE id = ?", originalPublishedAt, noteId);

        mockMvc.perform(post("/api/v1/notes/{noteId}/unpublish", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(noteId.intValue()))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.publishedAt").exists());

        String visibility = jdbcTemplate.queryForObject("SELECT visibility FROM note WHERE id = ?", String.class, noteId);
        LocalDateTime publishedAt = jdbcTemplate.queryForObject("SELECT published_at FROM note WHERE id = ?", LocalDateTime.class, noteId);

        assertThat(visibility).isEqualTo("PRIVATE");
        assertThat(publishedAt).isEqualTo(originalPublishedAt);
    }

    @Test
    void unpublishNote_otherUsersNote_returns40401() throws Exception {
        AppUser owner = createEnabledUser("note_unpublish_owner_2", "NoteUnpublishOwner2", "USER");
        AppUser viewer = createEnabledUser("note_unpublish_viewer", "NoteUnpublishViewer", "USER");
        String viewerToken = tokenOf(viewer);

        Long noteId = insertNote(owner.getId(), "Owner Public Note", "owner content", "owner summary", 0, null);
        jdbcTemplate.update("UPDATE note SET visibility = 'PUBLIC', published_at = ? WHERE id = ?", LocalDateTime.of(2026, 1, 2, 11, 0), noteId);

        mockMvc.perform(post("/api/v1/notes/{noteId}/unpublish", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + viewerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void unpublishNote_deletedNote_returns40401() throws Exception {
        AppUser user = createEnabledUser("note_unpublish_deleted", "NoteUnpublishDeleted", "USER");
        String token = tokenOf(user);

        Long noteId = createDeletedNote(user.getId(), "Deleted Public Note", "deleted content", "deleted summary");
        jdbcTemplate.update("UPDATE note SET visibility = 'PUBLIC', published_at = ? WHERE id = ?", LocalDateTime.now(), noteId);

        mockMvc.perform(post("/api/v1/notes/{noteId}/unpublish", noteId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }
}
