package com.yousheng.knowledgehub.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yousheng.knowledgehub.security.JwtConstants;
import com.yousheng.knowledgehub.support.ControllerBehaviorTestSupport;
import com.yousheng.knowledgehub.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileControllerBehaviorTest extends ControllerBehaviorTestSupport {

    @Test
    void getMe_success() throws Exception {
        AppUser user = createEnabledUser("meprofile", "Me Profile", "USER");
        String token = tokenOf(user);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("meprofile"))
                .andExpect(jsonPath("$.data.nickname").value("Me Profile"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    void getMe_withoutToken_returns40100() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void getMe_disabledUser_returns40301() throws Exception {
        AppUser user = createDisabledUser("medisabled", "Me Disabled", "USER");
        String token = tokenOf(user);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void updateMe_success() throws Exception {
        AppUser user = createEnabledUser("meupdate", "Me Update", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "nickname": "Updated Nick",
                  "bio": "My new bio"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("meupdate"))
                .andExpect(jsonPath("$.data.nickname").value("Updated Nick"))
                .andExpect(jsonPath("$.data.bio").value("My new bio"));
    }

    @Test
    void updateMe_withoutToken_returns40100() throws Exception {
        String body = """
                {
                  "nickname": "Updated",
                  "bio": "bio"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void updateMe_blankNickname_returns40001() throws Exception {
        AppUser user = createEnabledUser("meblank", "Me Blank", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "nickname": "   ",
                  "bio": "some bio"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void updateMe_tooLongBio_returns40001() throws Exception {
        AppUser user = createEnabledUser("melongbio", "Me LongBio", "USER");
        String token = tokenOf(user);

        String longBio = "a".repeat(61);
        String body = """
                {
                  "nickname": "Valid Nick",
                  "bio": "%s"
                }
                """.formatted(longBio);

        mockMvc.perform(put("/api/v1/users/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void updateMe_doesNotChangeUsernameRoleStatus() throws Exception {
        AppUser user = createEnabledUser("meimmutable", "Me Immutable", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "nickname": "Changed Nick",
                  "bio": "Changed bio"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("meimmutable"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andExpect(jsonPath("$.data.nickname").value("Changed Nick"))
                .andExpect(jsonPath("$.data.bio").value("Changed bio"));
    }

    @Test
    void updateMe_onlyBio_preservesNickname() throws Exception {
        AppUser user = createEnabledUser("mebioonly", "Only Bio", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "bio": "Only update bio"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("Only Bio"))
                .andExpect(jsonPath("$.data.bio").value("Only update bio"));
    }

    @Test
    void updateMe_onlyNickname_preservesBio() throws Exception {
        AppUser user = createEnabledUser("menickonly", "Only Nick", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "nickname": "New Nickname"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("New Nickname"))
                .andExpect(jsonPath("$.data.bio").doesNotExist());
    }

    @Test
    void updateMe_disabledUser_returns40301() throws Exception {
        AppUser user = createDisabledUser("medisupd", "Me Disabled Update", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "nickname": "New Nick",
                  "bio": "New bio"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void updateMe_emptyBody_preservesAll() throws Exception {
        AppUser user = createEnabledUser("meempty", "Me Empty", "USER");
        String token = tokenOf(user);

        mockMvc.perform(put("/api/v1/users/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("meempty"))
                .andExpect(jsonPath("$.data.nickname").value("Me Empty"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));
    }

    // ---- updatePassword ----

    @Test
    void updatePassword_success() throws Exception {
        String token = registerAndGetToken("pwduser1", "OldPass123", "Pwd User 1");

        String body = """
                {
                  "oldPassword": "OldPass123",
                  "newPassword": "NewPass456"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void updatePassword_wrongOldPassword_returns40102() throws Exception {
        String token = registerAndGetToken("pwduser2", "OldPass123", "Pwd User 2");

        String body = """
                {
                  "oldPassword": "WrongOldPass",
                  "newPassword": "NewPass456"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));
    }

    @Test
    void updatePassword_withoutToken_returns40100() throws Exception {
        String body = """
                {
                  "oldPassword": "OldPass123",
                  "newPassword": "NewPass456"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void updatePassword_disabledUser_returns40301() throws Exception {
        String token = registerAndGetToken("pwduser3", "OldPass123", "Pwd User 3");

        AppUser user = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>()
                        .eq(AppUser::getUsername, "pwduser3"));
        user.setStatus("DISABLED");
        appUserMapper.updateById(user);

        String body = """
                {
                  "oldPassword": "OldPass123",
                  "newPassword": "NewPass456"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void updatePassword_newPasswordTooShort_returns40001() throws Exception {
        String token = registerAndGetToken("pwduser4", "OldPass123", "Pwd User 4");

        String body = """
                {
                  "oldPassword": "OldPass123",
                  "newPassword": "Short1"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void updatePassword_oldPasswordTooShort_returns40001() throws Exception {
        String token = registerAndGetToken("pwduser5", "OldPass123", "Pwd User 5");

        String body = """
                {
                  "oldPassword": "Short1",
                  "newPassword": "NewPass456"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void updatePassword_canLoginWithNewPassword() throws Exception {
        String token = registerAndGetToken("pwduser6", "OldPass123", "Pwd User 6");

        String body = """
                {
                  "oldPassword": "OldPass123",
                  "newPassword": "NewPass456"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // can login with new password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "pwduser6",
                                  "password": "NewPass456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void updatePassword_cannotLoginWithOldPassword() throws Exception {
        String token = registerAndGetToken("pwduser7", "OldPass123", "Pwd User 7");

        String body = """
                {
                  "oldPassword": "OldPass123",
                  "newPassword": "NewPass456"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // cannot login with old password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "pwduser7",
                                  "password": "OldPass123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));
    }

    // ---- helpers ----

    private String registerAndGetToken(String username, String password, String nickname) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "nickname": "%s",
                                  "inviteCode": "test-invite-code"
                                }
                                """.formatted(username, password, nickname)))
                .andExpect(status().isOk());

        AppUser user = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>()
                        .eq(AppUser::getUsername, username));

        return jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()).accessToken();
    }
}
