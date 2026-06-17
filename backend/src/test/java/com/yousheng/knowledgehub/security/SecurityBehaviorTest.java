package com.yousheng.knowledgehub.security;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityBehaviorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void protectedApi_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/test/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedApi_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/test/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedApi_withUserToken_returns200() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(1L, "alice", "USER").accessToken();

        mockMvc.perform(get("/api/v1/test/me")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminApi_withUserToken_returns403() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(1L, "alice", "USER").accessToken();

        mockMvc.perform(get("/api/v1/admin/test")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminApi_withAdminToken_returns200() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(2L, "admin", "ADMIN").accessToken();

        mockMvc.perform(get("/api/v1/admin/test")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class SecurityTestControllerConfig {

        @Bean
        TestSecurityController testSecurityController() {
            return new TestSecurityController();
        }
    }

    @RestController
    static class TestSecurityController {

        @GetMapping("/api/v1/test/me")
        ApiResponse<CurrentUserPrincipal> me() {
            return ApiResponse.ok(CurrentUser.getPrincipal());
        }

        @GetMapping("/api/v1/admin/test")
        ApiResponse<String> admin() {
            return ApiResponse.ok("admin ok");
        }
    }
}