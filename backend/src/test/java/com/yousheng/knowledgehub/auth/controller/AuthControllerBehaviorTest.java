package com.yousheng.knowledgehub.auth.controller;

import com.yousheng.knowledgehub.security.JwtConstants;
import com.yousheng.knowledgehub.security.JwtTokenProvider;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.invite-code=test-invite-code")
class AuthControllerBehaviorTest {

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
        jdbcTemplate.execute("DELETE FROM app_user");
    }

    @Test
    void authMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authMe_withValidToken_returns200() throws Exception {
        AppUser user = createEnabledUser("me200_user", "Me200", "USER");
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk());
    }

    @Test
    void authMe_returnsIdUsernameNicknameRole() throws Exception {
        AppUser user = createEnabledUser("me_fields_user", "MeFields", "USER");
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(user.getId().intValue()))
                .andExpect(jsonPath("$.data.username").value("me_fields_user"))
                .andExpect(jsonPath("$.data.nickname").value("MeFields"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void register_withWrongInviteCode_returns40002() throws Exception {
        String body = """
                {
                  "username": "wrong_code_user",
                  "password": "Password123",
                  "nickname": "WrongCode",
                  "inviteCode": "bad-invite-code"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));
    }

    private AppUser createEnabledUser(String username, String nickname, String role) {
        LocalDateTime now = LocalDateTime.now();

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash("unused-hash");
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus("ENABLED");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        appUserMapper.insert(user);
        return user;
    }
}
