package com.yousheng.knowledgehub.ai.rag.controller;

import com.yousheng.knowledgehub.ai.rag.AiRagAnswer;
import com.yousheng.knowledgehub.ai.rag.AiRagService;
import com.yousheng.knowledgehub.ai.rag.AiRagSource;
import com.yousheng.knowledgehub.security.JwtConstants;
import com.yousheng.knowledgehub.support.ControllerBehaviorTestSupport;
import com.yousheng.knowledgehub.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.ai.enabled=true",
        "spring.ai.model.embedding=openai",
        "spring.ai.model.chat=openai",
        "app.ai.index.vector-store=redis",
        "app.ai.rag.enabled=true",
        "spring.ai.openai.chat.api-key=sk-test-key",
        "spring.ai.openai.chat.base-url=https://test.example.com",
        "spring.ai.openai.chat.options.model=test-model"
})
class AiRagControllerBehaviorTest extends ControllerBehaviorTestSupport {

    @MockBean
    private AiRagService aiRagService;

    @MockBean
    private EmbeddingModel embeddingModel;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private ChatModel chatModel;

    @Test
    void ask_loggedInUser_returnsAnswerWithSources() throws Exception {
        AppUser user = createEnabledUser("raguser", "RAG User", "USER");
        String token = tokenOf(user);

        AiRagSource source = new AiRagSource(
                10L, "My Note", "chunk content", 0, 0.92, "public",
                LocalDateTime.of(2026, 6, 24, 2, 0, 0));
        AiRagAnswer answer = new AiRagAnswer("LLM answer", List.of(source));

        when(aiRagService.ask("what is AI?")).thenReturn(answer);

        mockMvc.perform(post("/api/v1/ai/rag/ask")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content("{\"question\":\"what is AI?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.answer").value("LLM answer"))
                .andExpect(jsonPath("$.data.sources[0].noteId").value(10))
                .andExpect(jsonPath("$.data.sources[0].title").value("My Note"))
                .andExpect(jsonPath("$.data.sources[0].snippet").value("chunk content"))
                .andExpect(jsonPath("$.data.sources[0].chunkIndex").value(0))
                .andExpect(jsonPath("$.data.sources[0].distance").value(0.92))
                .andExpect(jsonPath("$.data.sources[0].visibility").value("public"))
                .andExpect(jsonPath("$.data.sources[0].updatedAt").value("2026-06-24T02:00:00"));

        verify(aiRagService).ask("what is AI?");
    }

    @Test
    void ask_passesQuestionToRagService() throws Exception {
        AppUser user = createEnabledUser("raguser2", "RAG User 2", "USER");
        String token = tokenOf(user);

        AiRagAnswer answer = new AiRagAnswer("ok", Collections.emptyList());
        when(aiRagService.ask(anyString())).thenReturn(answer);

        mockMvc.perform(post("/api/v1/ai/rag/ask")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content("{\"question\":\"Explain Spring AI\"}"))
                .andExpect(status().isOk());

        verify(aiRagService).ask("Explain Spring AI");
    }

    @Test
    void ask_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/rag/ask")
                        .contentType("application/json")
                        .content("{\"question\":\"test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void ask_blankQuestion_returns400() throws Exception {
        AppUser user = createEnabledUser("raguser3", "RAG User 3", "USER");
        String token = tokenOf(user);

        mockMvc.perform(post("/api/v1/ai/rag/ask")
                        .header(JwtConstants.AUTHORIZATION_HEADER, JwtConstants.BEARER_PREFIX + token)
                        .contentType("application/json")
                        .content("{\"question\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
