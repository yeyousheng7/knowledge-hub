package com.yousheng.knowledgehub.user.controller;

import com.yousheng.knowledgehub.support.ControllerBehaviorTestSupport;
import com.yousheng.knowledgehub.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicUserControllerBehaviorTest extends ControllerBehaviorTestSupport {

    @Test
    void public_getUserProfile_success() throws Exception {
        AppUser user = createEnabledUser("testuser", "Test User", "USER");

        mockMvc.perform(get("/api/v1/public/users/{username}", user.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.nickname").value("Test User"))
                .andExpect(jsonPath("$.data.bio").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    void public_getUserProfile_userNotFound_returns40402() throws Exception {
        mockMvc.perform(get("/api/v1/public/users/{username}", "nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));
    }

    @Test
    void public_getUserProfile_disabledUser_returns40402() throws Exception {
        AppUser user = createDisabledUser("disableduser", "Disabled User", "USER");

        mockMvc.perform(get("/api/v1/public/users/{username}", user.getUsername()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));
    }

    @Test
    void public_getUserProfile_invalidUsername_returns40001() throws Exception {
        mockMvc.perform(get("/api/v1/public/users/{username}", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }
}
