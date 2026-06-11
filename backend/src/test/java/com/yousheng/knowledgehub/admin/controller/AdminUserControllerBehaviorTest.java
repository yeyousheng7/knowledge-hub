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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserControllerBehaviorTest {

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

    // ---- disable ----

    @Test
    void admin_disableUser_returns200() throws Exception {
        AppUser admin = createEnabledUser("admin_disable", "AdminDisable", "ADMIN");
        AppUser user = createEnabledUser("user_disable", "UserDisable", "USER");
        String adminToken = tokenOf(admin);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/disable", user.getId())
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(user.getId().intValue()))
                .andExpect(jsonPath("$.data.username").value("user_disable"))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        String dbStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM app_user WHERE id = ?", String.class, user.getId());

        assertEquals("DISABLED", dbStatus);
    }

    @Test
    void admin_disableUser_twice_isIdempotent() throws Exception {
        AppUser admin = createEnabledUser("admin_disable_2x", "AdminDisable2x", "ADMIN");
        AppUser user = createEnabledUser("user_disable_2x", "UserDisable2x", "USER");
        String adminToken = tokenOf(admin);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/disable", user.getId())
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/{userId}/disable", user.getId())
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        String dbStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM app_user WHERE id = ?", String.class, user.getId());

        assertEquals("DISABLED", dbStatus);
    }

    @Test
    void admin_disableSelf_returns403() throws Exception {
        AppUser admin = createEnabledUser("admin_self", "AdminSelf", "ADMIN");
        String adminToken = tokenOf(admin);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/disable", admin.getId())
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_disableUser_returns403() throws Exception {
        AppUser user = createEnabledUser("user_try_disable", "UserTryDisable", "USER");
        AppUser target = createEnabledUser("target_user", "TargetUser", "USER");
        String userToken = tokenOf(user);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/disable", target.getId())
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_disableUser_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/{userId}/disable", 1L))
                .andExpect(status().isUnauthorized());
    }

    // ---- enable ----

    @Test
    void admin_enableDisabledUser_returns200() throws Exception {
        AppUser admin = createEnabledUser("admin_enable", "AdminEnable", "ADMIN");
        AppUser user = createDisabledUser("user_disabled", "UserDisabled", "USER");
        String adminToken = tokenOf(admin);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/enable", user.getId())
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(user.getId().intValue()))
                .andExpect(jsonPath("$.data.username").value("user_disabled"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));

        String dbStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM app_user WHERE id = ?", String.class, user.getId());

        assertEquals("ENABLED", dbStatus);
    }

    @Test
    void admin_enableUser_twice_isIdempotent() throws Exception {
        AppUser admin = createEnabledUser("admin_enable_2x", "AdminEnable2x", "ADMIN");
        AppUser user = createDisabledUser("user_enable_2x", "UserEnable2x", "USER");
        String adminToken = tokenOf(admin);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/enable", user.getId())
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/{userId}/enable", user.getId())
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));

        String dbStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM app_user WHERE id = ?", String.class, user.getId());

        assertEquals("ENABLED", dbStatus);
    }

    // ---- list ----

    @Test
    void admin_listUsers_returnsPagedUsers() throws Exception {
        AppUser admin = createEnabledUser("admin_list", "AdminList", "ADMIN");
        String adminToken = tokenOf(admin);

        createEnabledUser("alice", "Alice", "USER");
        createEnabledUser("bob", "Bob", "USER");
        createEnabledUser("charlie", "Charlie", "USER");

        mockMvc.perform(get("/api/v1/admin/users?page=1&size=2")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void admin_listUsers_withStatus_returnsFilteredUsers() throws Exception {
        AppUser admin = createEnabledUser("admin_list_status", "AdminListStatus", "ADMIN");
        String adminToken = tokenOf(admin);

        createEnabledUser("alice", "Alice", "USER");
        createDisabledUser("bob", "Bob", "USER");
        createEnabledUser("charlie", "Charlie", "USER");

        mockMvc.perform(get("/api/v1/admin/users?page=1&size=20&status=DISABLED")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].username").value("bob"))
                .andExpect(jsonPath("$.data.items[0].status").value("DISABLED"));
    }

    @Test
    void admin_listUsers_withKeyword_returnsMatchedUsers() throws Exception {
        AppUser admin = createEnabledUser("admin_list_kw", "AdminListKw", "ADMIN");
        String adminToken = tokenOf(admin);

        createEnabledUser("spring_boot", "Spring Boot Dev", "USER");
        createEnabledUser("java_dev", "Java Developer", "USER");
        createEnabledUser("docker_user", "Docker User", "USER");

        mockMvc.perform(get("/api/v1/admin/users?page=1&size=20&keyword=spring")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].username").value("spring_boot"));
    }

    @Test
    void user_listUsers_returns403() throws Exception {
        AppUser user = createEnabledUser("user_list", "UserList", "USER");
        String userToken = tokenOf(user);

        mockMvc.perform(get("/api/v1/admin/users?page=1&size=20")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_listUsers_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users?page=1&size=20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAdmin_listUsers_returns40301() throws Exception {
        AppUser disabledAdmin = createDisabledUser("disabled_admin_list", "DisabledAdminList", "ADMIN");
        String adminToken = tokenOf(disabledAdmin);

        mockMvc.perform(get("/api/v1/admin/users?page=1&size=20")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
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
}
