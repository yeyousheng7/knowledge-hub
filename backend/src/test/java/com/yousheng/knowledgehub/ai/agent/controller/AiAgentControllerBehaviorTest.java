package com.yousheng.knowledgehub.ai.agent.controller;

import com.yousheng.knowledgehub.ai.agent.AiAgentChatService;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.JwtConstants;
import com.yousheng.knowledgehub.support.ControllerBehaviorTestSupport;
import com.yousheng.knowledgehub.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.ai.enabled=true",
        "app.ai.agent.enabled=true",
        "spring.ai.model.chat=openai",
        "app.ai.chat.provider=deepseek",
        "spring.ai.openai.chat.api-key=sk-test-key",
        "spring.ai.openai.chat.base-url=https://test.example.com",
        "spring.ai.openai.chat.options.model=test-model"
})
class AiAgentControllerBehaviorTest extends ControllerBehaviorTestSupport {

    @MockBean
    private AiAgentChatService aiAgentChatService;

    @MockBean
    private ChatModel chatModel;

    @Test
    void chat_loggedInUser_returnsAnswer() throws Exception {
        AppUser user = createEnabledUser("agentuser", "Agent User", "USER");
        String token = tokenOf(user);

        when(aiAgentChatService.chat("Hello")).thenReturn("Hi there!");

        mockMvc.perform(post("/api/v1/ai/agent/chat")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.answer").value("Hi there!"));

        verify(aiAgentChatService).chat("Hello");
    }

    @Test
    void chat_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/agent/chat")
                        .contentType("application/json")
                        .content("{\"message\":\"test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void chat_blankMessage_returns400() throws Exception {
        AppUser user = createEnabledUser("agentuser2", "Agent User 2", "USER");
        String token = tokenOf(user);

        mockMvc.perform(post("/api/v1/ai/agent/chat")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_messageTooLong_returns400() throws Exception {
        AppUser user = createEnabledUser("agentuser3", "Agent User 3", "USER");
        String token = tokenOf(user);

        String tooLong = "a".repeat(1001);

        mockMvc.perform(post("/api/v1/ai/agent/chat")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content("{\"message\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_serviceThrowsBizException_handledByGlobalExceptionHandler() throws Exception {
        AppUser user = createEnabledUser("agentuser4", "Agent User 4", "USER");
        String token = tokenOf(user);

        when(aiAgentChatService.chat(anyString()))
                .thenThrow(new BizException(ErrorCode.AI_CHAT_SERVICE_UNAVAILABLE));

        mockMvc.perform(post("/api/v1/ai/agent/chat")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content("{\"message\":\"crash\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(50303));
    }
}
