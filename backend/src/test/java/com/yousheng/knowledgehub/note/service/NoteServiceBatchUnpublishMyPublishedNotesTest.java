package com.yousheng.knowledgehub.note.service;

import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteBatchUnpublishResult;
import com.yousheng.knowledgehub.security.CurrentUserPrincipal;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NoteServiceBatchUnpublishMyPublishedNotesTest {

    @Autowired
    private NoteService noteService;

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM note_tag");
        jdbcTemplate.execute("DELETE FROM note");
        jdbcTemplate.execute("DELETE FROM tag");
        jdbcTemplate.execute("DELETE FROM category");
        jdbcTemplate.execute("DELETE FROM app_user");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void batchUnpublishMyPublishedNotes_successChangesNotesToPrivate() {
        AppUser user = createEnabledUser("batch_user", "Batch User");
        setCurrentUser(user);
        Long firstId = insertNote(user.getId(), "first", "PUBLIC", "NORMAL", LocalDateTime.now());
        Long secondId = insertNote(user.getId(), "second", "PUBLIC", "NORMAL", LocalDateTime.now());

        NoteBatchUnpublishResult result = noteService.batchUnpublishMyPublishedNotes(List.of(firstId, secondId));

        assertThat(result.affectedCount()).isEqualTo(2);
        assertThat(result.affectedItems()).extracting("id").containsExactly(firstId, secondId);
        assertThat(visibilityOf(firstId)).isEqualTo("PRIVATE");
        assertThat(visibilityOf(secondId)).isEqualTo("PRIVATE");
    }

    @Test
    void batchUnpublishMyPublishedNotes_whenAnyNoteNotPublic_failsWithoutPartialUnpublish() {
        AppUser user = createEnabledUser("batch_user2", "Batch User 2");
        setCurrentUser(user);
        Long publicId = insertNote(user.getId(), "public", "PUBLIC", "NORMAL", LocalDateTime.now());
        Long privateId = insertNote(user.getId(), "private", "PRIVATE", "NORMAL", null);

        assertThatThrownBy(() -> noteService.batchUnpublishMyPublishedNotes(List.of(publicId, privateId)))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.NOTE_NOT_FOUND));
        assertThat(visibilityOf(publicId)).isEqualTo("PUBLIC");
        assertThat(visibilityOf(privateId)).isEqualTo("PRIVATE");
    }

    private AppUser createEnabledUser(String username, String nickname) {
        LocalDateTime now = LocalDateTime.now();
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash("unused-hash");
        user.setNickname(nickname);
        user.setRole("USER");
        user.setStatus(UserStatus.ENABLED.name());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        appUserMapper.insert(user);
        return user;
    }

    private void setCurrentUser(AppUser user) {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(user.getId(), user.getUsername(), user.getRole());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Long insertNote(Long userId, String title, String visibility, String moderationStatus,
                            LocalDateTime publishedAt) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO note (
                                user_id, title, content_md, summary, visibility, category_id,
                                created_at, updated_at, published_at, moderation_status,
                                moderated_at, deleted, deleted_at
                        ) VALUES (?, ?, 'content', 'summary', ?, NULL, ?, ?, ?, ?, NULL, 0, NULL)
                        """,
                userId, title, visibility, now, now, publishedAt, moderationStatus);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM note", Long.class);
    }

    private String visibilityOf(Long noteId) {
        return jdbcTemplate.queryForObject("SELECT visibility FROM note WHERE id = ?", String.class, noteId);
    }
}
