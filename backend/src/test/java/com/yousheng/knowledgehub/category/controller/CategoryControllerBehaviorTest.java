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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    // ---- deleteCategory ----

    @Test
    void deleteCategory_withValidToken_deletesCategory() throws Exception {
        AppUser user = createEnabledUser("cat_del_1", "CatDel1", "USER");
        String token = tokenOf(user);

        Long categoryId = insertCategory(user.getId(), "To Delete");

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", categoryId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证删除后分类列表不再返回该分类
        mockMvc.perform(get("/api/v1/categories")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void deleteCategory_withNotes_clearsNoteCategoryId() throws Exception {
        AppUser user = createEnabledUser("cat_del_note", "CatDelNote", "USER");
        String token = tokenOf(user);

        Long categoryId = insertCategory(user.getId(), "With Notes");

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO note (user_id, title, content_md, summary, visibility, category_id,
                            created_at, updated_at, published_at, moderation_status,
                            moderated_at, deleted, deleted_at)
                        VALUES (?, ?, ?, ?, 'PRIVATE', ?, ?, ?, NULL, 'NORMAL', NULL, 0, NULL)
                        """,
                user.getId(), "Note In Cat", "content", "summary", categoryId, now, now
        );
        Long noteId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", categoryId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证该分类下的 Note 变成未分类
        Long dbCategoryId = jdbcTemplate.queryForObject(
                "SELECT category_id FROM note WHERE id = ?", Long.class, noteId);
        assertEquals(null, dbCategoryId);

        // 验证分类已软删除
        Integer dbDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM category WHERE id = ?", Integer.class, categoryId);
        assertEquals(1, dbDeleted);
    }

    @Test
    void deleteCategory_withOtherUserCategory_returns404() throws Exception {
        AppUser owner = createEnabledUser("cat_del_owner", "CatDelOwner", "USER");
        AppUser other = createEnabledUser("cat_del_other", "CatDelOther", "USER");
        String otherToken = tokenOf(other);

        Long ownerCategoryId = insertCategory(owner.getId(), "Owner Category");

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", ownerCategoryId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40403));
    }

    @Test
    void deleteCategory_thenRecreateWithSameName_succeeds() throws Exception {
        AppUser user = createEnabledUser("cat_del_recreate", "CatDelRecreate", "USER");
        String token = tokenOf(user);

        Long categoryId = insertCategory(user.getId(), "Spring");

        // 删除分类
        mockMvc.perform(delete("/api/v1/categories/{categoryId}", categoryId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 重新创建同名分类
        String body = """
                {
                  "name": "Spring"
                }
                """;
        mockMvc.perform(post("/api/v1/categories")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("Spring"));
    }

    @Test
    void deleteCategory_alreadyDeletedOrNotExists_returns40403() throws Exception {
        AppUser user = createEnabledUser("cat_del_404", "CatDel404", "USER");
        String token = tokenOf(user);

        Long categoryId = insertCategory(user.getId(), "Gone");

        // 第一次删除成功
        mockMvc.perform(delete("/api/v1/categories/{categoryId}", categoryId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk());

        // 重复删除已删除的分类
        mockMvc.perform(delete("/api/v1/categories/{categoryId}", categoryId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40403));

        // 删除不存在的分类
        mockMvc.perform(delete("/api/v1/categories/{categoryId}", 999_999L)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40403));
    }

    @Test
    void deleteCategory_withDisabledUser_returns403() throws Exception {
        AppUser user = createEnabledUser("cat_del_disabled", "CatDelDisabled", "USER");
        Long categoryId = insertCategory(user.getId(), "Disabled Cat");

        user.setStatus(UserStatus.DISABLED.name());
        appUserMapper.updateById(user);
        String token = tokenOf(user);

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", categoryId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    // ---- updateCategory ----

    @Test
    void updateCategory_withValidToken_updatesName() throws Exception {
        AppUser user = createEnabledUser("cat_upd_1", "CatUpd1", "USER");
        String token = tokenOf(user);

        Long categoryId = insertCategory(user.getId(), "Old Name");

        String body = """
                {
                  "name": "  New Name  "
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{categoryId}", categoryId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(categoryId.intValue()))
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());

        String dbName = jdbcTemplate.queryForObject(
                "SELECT name FROM category WHERE id = ? AND deleted = 0", String.class, categoryId);
        assertEquals("New Name", dbName);
    }

    @Test
    void updateCategory_withDuplicateName_returns409() throws Exception {
        AppUser user = createEnabledUser("cat_upd_dup", "CatUpdDup", "USER");
        String token = tokenOf(user);

        insertCategory(user.getId(), "Spring");
        Long categoryId = insertCategory(user.getId(), "Java");

        String body = """
                {
                  "name": "Spring"
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{categoryId}", categoryId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));
    }

    @Test
    void updateCategory_withOtherUserCategory_returns404() throws Exception {
        AppUser owner = createEnabledUser("cat_upd_owner", "CatUpdOwner", "USER");
        AppUser other = createEnabledUser("cat_upd_other", "CatUpdOther", "USER");
        String otherToken = tokenOf(other);

        Long ownerCategoryId = insertCategory(owner.getId(), "Owner Category");

        String body = """
                {
                  "name": "Hacked"
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{categoryId}", ownerCategoryId)
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + otherToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40403));
    }

    @Test
    void updateCategory_withoutToken_returns401() throws Exception {
        String body = """
                {
                  "name": "Unauth"
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{categoryId}", 1L)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
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

    private Long insertCategory(Long userId, String name) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO category (user_id, name, created_at, updated_at, deleted, deleted_marker, deleted_at) VALUES (?, ?, ?, ?, 0, 0, NULL)",
                userId,
                name,
                now,
                now
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
