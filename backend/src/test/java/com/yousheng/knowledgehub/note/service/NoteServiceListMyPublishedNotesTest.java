package com.yousheng.knowledgehub.note.service;

import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteListResponse;
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
class NoteServiceListMyPublishedNotesTest {

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
    void returnsCurrentUserPublishedNotes() {
        AppUser user = createEnabledUser("author", "Author");
        setCurrentUser(user);
        LocalDateTime now = LocalDateTime.now();
        insertNote(user.getId(), "My Published Note", "PRIVATE", "NORMAL", now, now, 0);
        insertPublishedNote(user.getId(), "My Published Note 1", now.minusDays(1));
        insertPublishedNote(user.getId(), "My Published Note 2", now);

        NoteListResponse result = noteService.listMyPublishedNotes(1, 5);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).title()).isEqualTo("My Published Note 2");
        assertThat(result.items().get(1).title()).isEqualTo("My Published Note 1");
        assertThat(result.total()).isEqualTo(2);
    }

    @Test
    void doesNotReturnPrivateNotes() {
        AppUser user = createEnabledUser("author2", "Author2");
        setCurrentUser(user);
        LocalDateTime now = LocalDateTime.now();
        insertPublishedNote(user.getId(), "Published", now);
        insertNote(user.getId(), "Private Note", "PRIVATE", "NORMAL", now, now, 0);

        NoteListResponse result = noteService.listMyPublishedNotes(1, 5);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("Published");
    }

    @Test
    void doesNotReturnPublishedWithNullPublishedAt() {
        AppUser user = createEnabledUser("author3", "Author3");
        setCurrentUser(user);
        LocalDateTime now = LocalDateTime.now();
        insertPublishedNote(user.getId(), "Good Published", now);
        jdbcTemplate.update(
                """
                        INSERT INTO note (
                                user_id, title, content_md, summary, visibility, category_id,
                                created_at, updated_at, published_at, moderation_status,
                                moderated_at, deleted, deleted_at
                        ) VALUES (?, ?, 'content', 'summary', 'PUBLIC', NULL, ?, ?, NULL, 'NORMAL', NULL, 0, NULL)
                        """,
                user.getId(), "Public No PublishedAt", now, now);

        NoteListResponse result = noteService.listMyPublishedNotes(1, 5);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("Good Published");
    }

    @Test
    void doesNotReturnTakenDownNotes() {
        AppUser user = createEnabledUser("author4", "Author4");
        setCurrentUser(user);
        LocalDateTime now = LocalDateTime.now();
        insertPublishedNote(user.getId(), "Good Published", now);
        insertNote(user.getId(), "Taken Down", "PUBLIC", "TAKEN_DOWN", now, now, 0);

        NoteListResponse result = noteService.listMyPublishedNotes(1, 5);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("Good Published");
    }

    @Test
    void doesNotReturnTakenDownNoteWithPublishedAt() {
        AppUser user = createEnabledUser("author4b", "Author4b");
        setCurrentUser(user);
        LocalDateTime now = LocalDateTime.now();
        insertPublishedNote(user.getId(), "Good Published", now);
        jdbcTemplate.update(
                """
                        INSERT INTO note (
                                user_id, title, content_md, summary, visibility, category_id,
                                created_at, updated_at, published_at, moderation_status,
                                moderated_at, deleted, deleted_at
                        ) VALUES (?, ?, 'content', 'summary', 'PUBLIC', NULL, ?, ?, ?, 'TAKEN_DOWN', NULL, 0, NULL)
                        """,
                user.getId(), "Taken Down With PublishedAt", now, now, now);

        NoteListResponse result = noteService.listMyPublishedNotes(1, 5);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("Good Published");
    }

    @Test
    void doesNotReturnDeletedNotes() {
        AppUser user = createEnabledUser("author5", "Author5");
        setCurrentUser(user);
        LocalDateTime now = LocalDateTime.now();
        insertPublishedNote(user.getId(), "Published", now);
        insertNote(user.getId(), "Deleted", "PUBLIC", "NORMAL", now, now, 1);

        NoteListResponse result = noteService.listMyPublishedNotes(1, 5);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("Published");
    }

    @Test
    void doesNotReturnDeletedNoteWithPublishedAt() {
        AppUser user = createEnabledUser("author5b", "Author5b");
        setCurrentUser(user);
        LocalDateTime now = LocalDateTime.now();
        insertPublishedNote(user.getId(), "Good Published", now);
        jdbcTemplate.update(
                """
                        INSERT INTO note (
                                user_id, title, content_md, summary, visibility, category_id,
                                created_at, updated_at, published_at, moderation_status,
                                moderated_at, deleted, deleted_at
                        ) VALUES (?, ?, 'content', 'summary', 'PUBLIC', NULL, ?, ?, ?, 'NORMAL', NULL, 1, NULL)
                        """,
                user.getId(), "Deleted With PublishedAt", now, now, now);

        NoteListResponse result = noteService.listMyPublishedNotes(1, 5);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("Good Published");
    }

    @Test
    void doesNotReturnOtherUserPublicNotes() {
        AppUser author = createEnabledUser("author6", "Author6");
        AppUser other = createEnabledUser("other", "Other");
        setCurrentUser(author);
        LocalDateTime now = LocalDateTime.now();
        insertPublishedNote(author.getId(), "My Published", now);
        insertPublishedNote(other.getId(), "Other Published", now);

        NoteListResponse result = noteService.listMyPublishedNotes(1, 5);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("My Published");
    }

    @Test
    void disabledUser_returnsUserDisabled() {
        AppUser user = createEnabledUser("author7", "Author7");
        user.setStatus(UserStatus.DISABLED.name());
        appUserMapper.updateById(user);
        setCurrentUser(user);

        assertThatThrownBy(() -> noteService.listMyPublishedNotes(1, 5))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizEx = (BizException) ex;
                    assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.USER_DISABLED);
                });
    }

    @Test
    void unauthenticated_returnsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> noteService.listMyPublishedNotes(1, 5))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizEx = (BizException) ex;
                    assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                });
    }

    @Test
    void pagination_firstPage_returnsCorrectItems() {
        AppUser user = createEnabledUser("author8", "Author8");
        setCurrentUser(user);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= 5; i++) {
            insertPublishedNote(user.getId(), "Note " + i, now.minusDays(i));
        }

        NoteListResponse result = noteService.listMyPublishedNotes(1, 2);

        assertThat(result.items()).hasSize(2);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(5);
        assertThat(result.items().get(0).title()).isEqualTo("Note 1"); // publishedAt desc
        assertThat(result.items().get(1).title()).isEqualTo("Note 2");
    }

    @Test
    void emptyResult_returnsEmptyList() {
        AppUser user = createEnabledUser("authorEmpty", "AuthorEmpty");
        setCurrentUser(user);

        NoteListResponse result = noteService.listMyPublishedNotes(1, 5);

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isZero();
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
        CurrentUserPrincipal principal = new CurrentUserPrincipal(
                user.getId(), user.getUsername(), user.getRole());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void insertPublishedNote(Long userId, String title, LocalDateTime publishedAt) {
        LocalDateTime now = LocalDateTime.now();
        insertNote(userId, title, "PUBLIC", "NORMAL", now, publishedAt, 0);
    }

    private void insertNote(Long userId, String title, String visibility, String moderationStatus,
                            LocalDateTime createdAt, LocalDateTime publishedAtOrUpdated, int deleted) {
        jdbcTemplate.update(
                """
                        INSERT INTO note (
                                user_id, title, content_md, summary, visibility, category_id,
                                created_at, updated_at, published_at, moderation_status,
                                moderated_at, deleted, deleted_at
                        ) VALUES (?, ?, 'content', 'summary', ?, NULL, ?, ?, ?, ?, NULL, ?, NULL)
                        """,
                userId,
                title,
                visibility,
                createdAt,
                publishedAtOrUpdated,
                "PUBLIC".equals(visibility) && deleted == 0 && "NORMAL".equals(moderationStatus)
                        ? publishedAtOrUpdated : null,
                moderationStatus,
                deleted
        );
    }
}
