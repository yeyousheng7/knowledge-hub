package com.yousheng.knowledgehub.category.controller;

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
class CategoryControllerBehaviorTest {

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
        jdbcTemplate.execute("DELETE FROM note");
        jdbcTemplate.execute("DELETE FROM category");
        jdbcTemplate.execute("DELETE FROM app_user");
    }

    // ---- createCategory ----

    @Test
    void createCategory_withValidToken_returnsCreatedCategory() throws Exception {
        AppUser user = createEnabledUser("cat_user_1", "CatUser", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "name": "  Java  "
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("Java"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category WHERE user_id = ? AND name = ? AND deleted = 0",
                Integer.class,
                user.getId(),
                "Java"
        );

        assertEquals(1, count);
    }

    @Test
    void createCategory_withDuplicateName_returns409() throws Exception {
        AppUser user = createEnabledUser("cat_user_2", "CatUser2", "USER");
        String token = tokenOf(user);

        insertCategory(user.getId(), "Spring");

        String body = """
                {
                  "name": "Spring"
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));
    }

    @Test
    void createCategory_withBlankName_returns400() throws Exception {
        AppUser user = createEnabledUser("cat_user_3", "CatUser3", "USER");
        String token = tokenOf(user);

        String body = """
                {
                  "name": "   "
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void createCategory_withoutToken_returns401() throws Exception {
        String body = """
                {
                  "name": "Unauth"
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    // ---- listCategories ----

    @Test
    void listCategories_withValidToken_returnsOwnCategories() throws Exception {
        AppUser user = createEnabledUser("cat_user_4", "CatUser4", "USER");
        String token = tokenOf(user);

        insertCategory(user.getId(), "Spring");
        insertCategory(user.getId(), "Java");

        mockMvc.perform(get("/api/v1/categories")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].name").value("Java"))
                .andExpect(jsonPath("$.data.items[1].name").value("Spring"));
    }

    @Test
    void listCategories_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void listCategories_doesNotReturnOtherUsersCategories() throws Exception {
        AppUser owner = createEnabledUser("cat_user_5", "CatUser5", "USER");
        AppUser other = createEnabledUser("cat_user_6", "CatUser6", "USER");
        String ownerToken = tokenOf(owner);

        insertCategory(owner.getId(), "Owner Cat");
        insertCategory(other.getId(), "Other Cat");

        mockMvc.perform(get("/api/v1/categories")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("Owner Cat"));
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

    private String tokenOf(AppUser user) {
        return jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        ).accessToken();
    }

    private void insertCategory(Long userId, String name) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO category (user_id, name, created_at, updated_at, deleted, deleted_marker, deleted_at) VALUES (?, ?, ?, ?, 0, 0, NULL)",
                userId,
                name,
                now,
                now
        );
    }
}
