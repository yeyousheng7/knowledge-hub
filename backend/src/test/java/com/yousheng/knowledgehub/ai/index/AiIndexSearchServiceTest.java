package com.yousheng.knowledgehub.ai.index;

import com.yousheng.knowledgehub.ai.config.AiProperties;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiIndexSearchServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private AiIndexGenerationService generationService;

    @Mock
    private AiProperties aiProperties;

    @Mock
    private AiProperties.Index indexProperties;

    @Mock
    private AppUserMapper appUserMapper;

    private AiIndexSearchService service;

    @BeforeEach
    void setUp() {
        when(aiProperties.getIndex()).thenReturn(indexProperties);
        when(indexProperties.getTopK()).thenReturn(3);
        service = new AiIndexSearchService(vectorStore, generationService, aiProperties, appUserMapper);
    }

    private static AppUser enabledUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setStatus("ENABLED");
        return user;
    }

    // --- 1. query null / blank ---
    @Test
    void search_nullQuery_throwsIllegalArgumentException() {
        try (var ignored = mockStatic(CurrentUser.class)) {
            when(CurrentUser.getUserId()).thenReturn(1L);
            when(appUserMapper.selectById(1L)).thenReturn(enabledUser());

            assertThatThrownBy(() -> service.search(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("query must not be blank");
        }
    }

    @Test
    void search_blankQuery_throwsIllegalArgumentException() {
        try (var ignored = mockStatic(CurrentUser.class)) {
            when(CurrentUser.getUserId()).thenReturn(1L);
            when(appUserMapper.selectById(1L)).thenReturn(enabledUser());

            assertThatThrownBy(() -> service.search("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("query must not be blank");
        }
    }

    // --- 3. activeGeneration empty ---
    @Test
    void search_noActiveGeneration_returnsEmptyHitsWithoutCallingVectorStore() {
        try (var ignored = mockStatic(CurrentUser.class)) {
            when(CurrentUser.getUserId()).thenReturn(1L);
            when(appUserMapper.selectById(1L)).thenReturn(enabledUser());
            when(generationService.getActiveGeneration(1L)).thenReturn(null);

            AiIndexSearchResult result = service.search("hello");

            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.query()).isEqualTo("hello");
            assertThat(result.activeGeneration()).isNull();
            assertThat(result.hits()).isEmpty();

            verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
        }
    }

    // --- 4. normal scenario ---
    @Test
    void search_normal_returnsHitsWithCorrectFieldsAndFilter() {
        try (var ignored = mockStatic(CurrentUser.class)) {
            when(CurrentUser.getUserId()).thenReturn(1L);
            when(appUserMapper.selectById(1L)).thenReturn(enabledUser());
            when(generationService.getActiveGeneration(1L)).thenReturn("gen-1");

            Document doc = Document.builder()
                    .id("note:10:chunk:0")
                    .text("chunk text content")
                    .metadata(Map.of(
                            "noteId", 10L,
                            "title", "Test Note",
                            "chunkIndex", 0,
                            "distance", 0.92,
                            "visibility", "public",
                            "updatedAt", "2024-06-01T12:00:00"
                    ))
                    .build();

            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

            AiIndexSearchResult result = service.search(" hello ");

            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.query()).isEqualTo("hello");
            assertThat(result.activeGeneration()).isEqualTo("gen-1");
            assertThat(result.hits()).hasSize(1);

            AiIndexSearchHit hit = result.hits().get(0);
            assertThat(hit.noteId()).isEqualTo(10L);
            assertThat(hit.title()).isEqualTo("Test Note");
            assertThat(hit.chunkText()).isEqualTo("chunk text content");
            assertThat(hit.chunkIndex()).isEqualTo(0);
            assertThat(hit.distance()).isEqualTo(0.92);
            assertThat(hit.visibility()).isEqualTo("public");
            assertThat(hit.updatedAt()).isEqualTo("2024-06-01T12:00:00");

            ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
            verify(vectorStore).similaritySearch(captor.capture());
            SearchRequest captured = captor.getValue();
            assertThat(captured.getQuery()).isEqualTo("hello");
            assertThat(captured.getTopK()).isEqualTo(3);
            assertThat(captured.getFilterExpression()).isNotNull();
        }
    }

    @Test
    void search_distanceFromScoreKey_isExtracted() {
        try (var ignored = mockStatic(CurrentUser.class)) {
            when(CurrentUser.getUserId()).thenReturn(1L);
            when(appUserMapper.selectById(1L)).thenReturn(enabledUser());
            when(generationService.getActiveGeneration(1L)).thenReturn("gen-1");

            Document doc = Document.builder()
                    .id("note:1:chunk:0")
                    .text("text")
                    .metadata(Map.of(
                            "noteId", 1L,
                            "title", "T",
                            "chunkIndex", 0,
                            "score", 0.88
                    ))
                    .build();

            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

            AiIndexSearchResult result = service.search("q");

            assertThat(result.hits()).hasSize(1);
            assertThat(result.hits().get(0).distance()).isEqualTo(0.88);
        }
    }

    // --- 5. generationService throws ---
    @Test
    void search_generationServiceThrows_wrapsBizException() {
        try (var ignored = mockStatic(CurrentUser.class)) {
            when(CurrentUser.getUserId()).thenReturn(1L);
            when(appUserMapper.selectById(1L)).thenReturn(enabledUser());
            when(generationService.getActiveGeneration(1L)).thenThrow(new RuntimeException("redis down"));

            assertThatThrownBy(() -> service.search("hello"))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> {
                        BizException biz = (BizException) ex;
                        assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE);
                        assertThat(biz.getCause()).isNotNull();
                        assertThat(biz.getCause().getMessage()).contains("redis down");
                    });
        }
    }

    // --- 6. vectorStore.similaritySearch throws ---
    @Test
    void search_vectorStoreThrows_wrapsBizException() {
        try (var ignored = mockStatic(CurrentUser.class)) {
            when(CurrentUser.getUserId()).thenReturn(1L);
            when(appUserMapper.selectById(1L)).thenReturn(enabledUser());
            when(generationService.getActiveGeneration(1L)).thenReturn("gen-1");
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenThrow(new RuntimeException("vector store error"));

            assertThatThrownBy(() -> service.search("hello"))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> {
                        BizException biz = (BizException) ex;
                        assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE);
                        assertThat(biz.getCause()).isNotNull();
                        assertThat(biz.getCause().getMessage()).contains("vector store error");
                    });
        }
    }

    // --- edge case: empty documents list ---
    @Test
    void search_emptyDocuments_returnsEmptyHits() {
        try (var ignored = mockStatic(CurrentUser.class)) {
            when(CurrentUser.getUserId()).thenReturn(1L);
            when(appUserMapper.selectById(1L)).thenReturn(enabledUser());
            when(generationService.getActiveGeneration(1L)).thenReturn("gen-1");
            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

            AiIndexSearchResult result = service.search("hello");

            assertThat(result.hits()).isEmpty();
            assertThat(result.activeGeneration()).isEqualTo("gen-1");
        }
    }

    // --- user not found ---
    @Test
    void search_userNotFound_throwsUnauthorized() {
        try (var ignored = mockStatic(CurrentUser.class)) {
            when(CurrentUser.getUserId()).thenReturn(1L);
            when(appUserMapper.selectById(1L)).thenReturn(null);

            assertThatThrownBy(() -> service.search("hello"))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> {
                        BizException biz = (BizException) ex;
                        assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    });
        }
    }

    // --- user disabled ---
    @Test
    void search_userDisabled_throwsUserDisabled() {
        try (var ignored = mockStatic(CurrentUser.class)) {
            when(CurrentUser.getUserId()).thenReturn(1L);

            AppUser disabledUser = new AppUser();
            disabledUser.setId(1L);
            disabledUser.setStatus("DISABLED");
            when(appUserMapper.selectById(1L)).thenReturn(disabledUser);

            assertThatThrownBy(() -> service.search("hello"))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> {
                        BizException biz = (BizException) ex;
                        assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.USER_DISABLED);
                    });
        }
    }
}
