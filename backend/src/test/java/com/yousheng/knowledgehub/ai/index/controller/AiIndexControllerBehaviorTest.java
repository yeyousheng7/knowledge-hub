package com.yousheng.knowledgehub.ai.index.controller;

import com.yousheng.knowledgehub.ai.index.AiIndexWriteResult;
import com.yousheng.knowledgehub.ai.index.AiIndexWriterService;
import com.yousheng.knowledgehub.security.JwtConstants;
import com.yousheng.knowledgehub.support.ControllerBehaviorTestSupport;
import com.yousheng.knowledgehub.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.ai.enabled=true",
        "spring.ai.model.embedding=openai",
        "app.ai.index.vector-store=redis"
})
class AiIndexControllerBehaviorTest extends ControllerBehaviorTestSupport {

    @MockBean
    private AiIndexWriterService aiIndexWriterService;

    @MockBean
    private EmbeddingModel embeddingModel;

    @MockBean
    private VectorStore vectorStore;

    @Test
    void rebuild_loggedInUser_returnsResultAndUsesCurrentUserId() throws Exception {
        AppUser user = createEnabledUser("aiindexuser", "AI Index User", "USER");
        String token = tokenOf(user);
        Instant indexedAt = Instant.parse("2026-06-24T02:00:00Z");

        when(aiIndexWriterService.rebuildUserIndex(user.getId()))
                .thenReturn(new AiIndexWriteResult(user.getId(), 3, indexedAt));

        mockMvc.perform(post("/api/v1/ai/index/rebuild")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.chunkCount").value(3))
                .andExpect(jsonPath("$.data.indexedAt").value("2026-06-24T02:00:00Z"));

        verify(aiIndexWriterService).rebuildUserIndex(user.getId());
    }

    @Test
    void rebuild_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/index/rebuild"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void rebuild_disabledUser_returns403AndDoesNotCallWriter() throws Exception {
        AppUser user = createDisabledUser("aiindexdisabled", "AI Index Disabled", "USER");
        String token = tokenOf(user);

        mockMvc.perform(post("/api/v1/ai/index/rebuild")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        verify(aiIndexWriterService, never()).rebuildUserIndex(any());
    }
}
