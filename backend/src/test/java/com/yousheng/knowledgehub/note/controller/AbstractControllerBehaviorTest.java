package com.yousheng.knowledgehub.note.controller;

import com.yousheng.knowledgehub.security.JwtTokenProvider;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractControllerBehaviorTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected AppUserMapper appUserMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM note_tag");
        jdbcTemplate.execute("DELETE FROM note");
        jdbcTemplate.execute("DELETE FROM tag");
        jdbcTemplate.execute("DELETE FROM category");
        jdbcTemplate.execute("DELETE FROM app_user");
    }

    protected AppUser createEnabledUser(String username, String nickname, String role) {
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

    protected AppUser createDisabledUser(String username, String nickname, String role) {
        AppUser user = createEnabledUser(username, nickname, role);
        user.setStatus(UserStatus.DISABLED.name());
        appUserMapper.updateById(user);
        return user;
    }

    protected String tokenOf(AppUser user) {
        return jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        ).accessToken();
    }

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
