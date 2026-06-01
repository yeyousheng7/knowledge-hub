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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}