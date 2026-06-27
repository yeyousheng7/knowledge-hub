package com.yousheng.knowledgehub.ai.tool.rag;

import com.yousheng.knowledgehub.ai.config.AiProperties;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchHit;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchResult;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchService;
import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.ai.tool.rag.dto.RagNoteToolSearchResult;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagNoteToolFacadeTest {

    @Mock
    private AiIndexSearchService searchService;

    private AiProperties aiProperties;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        AiProperties.Index index = new AiProperties.Index();
        index.setTopK(3);
        aiProperties.setIndex(index);
    }

    private RagNoteToolFacade facadeWithSearchService() {
        @SuppressWarnings("unchecked")
        ObjectProvider<AiIndexSearchService> provider = Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(searchService);
        return new RagNoteToolFacade(provider, aiProperties);
    }

    private RagNoteToolFacade facadeWithoutSearchService() {
        @SuppressWarnings("unchecked")
        ObjectProvider<AiIndexSearchService> provider = Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return new RagNoteToolFacade(provider, aiProperties);
    }

    @Test
    void searchMyNotes_whenRagAvailable_returnsHits() {
        AiIndexSearchHit hit = new AiIndexSearchHit(
                1L, "Spring Boot Guide", "Spring Boot makes it easy to create stand-alone applications.",
                0, 0.85, "PUBLIC", LocalDateTime.now());
        AiIndexSearchResult searchResult = new AiIndexSearchResult(1L, "spring boot", "gen-001", List.of(hit));
        when(searchService.search("spring boot", 3)).thenReturn(searchResult);
        RagNoteToolFacade facade = facadeWithSearchService();

        AiToolResult<RagNoteToolSearchResult> result = facade.searchMyNotes("spring boot", null);

        assertThat(result.success()).isTrue();
        assertThat(result.data().hits()).hasSize(1);
        assertThat(result.data().hits().get(0).noteId()).isEqualTo(1L);
        assertThat(result.data().hits().get(0).title()).isEqualTo("Spring Boot Guide");
        assertThat(result.data().hits().get(0).snippet()).contains("Spring Boot");
        assertThat(result.data().hits().get(0).chunkIndex()).isEqualTo(0);
        assertThat(result.data().hits().get(0).distance()).isEqualTo(0.85);
    }

    @Test
    void searchMyNotes_whenRagUnavailable_returnsFailure() {
        RagNoteToolFacade facade = facadeWithoutSearchService();

        AiToolResult<RagNoteToolSearchResult> result = facade.searchMyNotes("spring boot", null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE.getCode());
    }

    @Test
    void searchMyNotes_blankQuery_returnsBadRequest() {
        RagNoteToolFacade facade = facadeWithSearchService();

        AiToolResult<RagNoteToolSearchResult> result = facade.searchMyNotes("   ", null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void searchMyNotes_topKExceeded_clampsAndWarns() {
        AiIndexSearchResult searchResult = new AiIndexSearchResult(1L, "test", "gen-001", List.of());
        when(searchService.search("test", 10)).thenReturn(searchResult);
        RagNoteToolFacade facade = facadeWithSearchService();

        AiToolResult<RagNoteToolSearchResult> result = facade.searchMyNotes("test", 20);

        assertThat(result.success()).isTrue();
        assertThat(result.data().topK()).isEqualTo(10);
        assertThat(result.warnings()).anyMatch(w -> w.contains("10"));
    }

    @Test
    void searchMyNotes_configTopKExceeded_clampsAndWarns() {
        aiProperties.getIndex().setTopK(100);
        AiIndexSearchResult searchResult = new AiIndexSearchResult(1L, "test", "gen-001", List.of());
        when(searchService.search("test", 10)).thenReturn(searchResult);
        RagNoteToolFacade facade = facadeWithSearchService();

        AiToolResult<RagNoteToolSearchResult> result = facade.searchMyNotes("test", null);

        assertThat(result.success()).isTrue();
        assertThat(result.data().topK()).isEqualTo(10);
        assertThat(result.warnings()).anyMatch(w -> w.contains("10"));
    }

    @Test
    void searchMyNotes_bizException_convertsToFailure() {
        BizException bizEx = new BizException(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE);
        when(searchService.search("test", 5)).thenThrow(bizEx);
        RagNoteToolFacade facade = facadeWithSearchService();

        AiToolResult<RagNoteToolSearchResult> result = facade.searchMyNotes("test", 5);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE.getCode());
    }
}
