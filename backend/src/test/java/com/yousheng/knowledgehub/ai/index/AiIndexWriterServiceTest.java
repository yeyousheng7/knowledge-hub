package com.yousheng.knowledgehub.ai.index;

import com.yousheng.knowledgehub.ai.index.model.NoteChunk;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import redis.clients.jedis.exceptions.JedisException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiIndexWriterServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private AiNoteIndexSourceService sourceService;

    @Mock
    private NoteChunkDocumentMapper documentMapper;

    @Mock
    private AiIndexGenerationService generationService;

    @Test
    void rebuildUserIndex_nullUserId_throwsException() {
        AiIndexWriterService service = service();

        assertThatThrownBy(() -> service.rebuildUserIndex(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId must not be null");
    }

    @Test
    void rebuildUserIndex_loadChunksThrows_doesNotDeleteOrAddDocuments() {
        Long userId = 10L;
        RuntimeException failure = new IllegalStateException("load failed");

        when(sourceService.loadChunks(userId)).thenThrow(failure);

        assertThatThrownBy(() -> service().rebuildUserIndex(userId))
                .isSameAs(failure);

        verify(documentMapper, never()).toDocuments(anyList());
        verify(vectorStore, never()).delete(any(Filter.Expression.class));
        verify(vectorStore, never()).add(anyList());
        verifyNoInteractions(generationService);
    }

    @Test
    void rebuildUserIndex_mapperThrows_doesNotAddActivateOrCleanup() {
        Long userId = 10L;
        List<NoteChunk> chunks = List.of(chunk("note:1:chunk:0", userId));
        RuntimeException failure = new IllegalStateException("map failed");

        when(sourceService.loadChunks(userId)).thenReturn(chunks);
        when(documentMapper.toDocuments(chunks)).thenThrow(failure);

        assertThatThrownBy(() -> service().rebuildUserIndex(userId))
                .isSameAs(failure);

        verify(vectorStore, never()).delete(any(Filter.Expression.class));
        verify(vectorStore, never()).add(anyList());
        verify(generationService, never()).getActiveGeneration(any());
        verify(generationService, never()).activateGeneration(any(), any());
    }

    @Test
    void rebuildUserIndex_addThrowsJedisException_doesNotActivateOrCleanup() {
        Long userId = 10L;
        List<NoteChunk> chunks = List.of(chunk("note:1:chunk:0", userId));
        List<Document> documents = List.of(document("note:1:chunk:0"));
        JedisException failure = new JedisException("redis down");

        when(sourceService.loadChunks(userId)).thenReturn(chunks);
        when(documentMapper.toDocuments(chunks)).thenReturn(documents);
        when(generationService.getActiveGeneration(userId)).thenReturn("gen-old");
        doThrow(failure).when(vectorStore).add(anyList());

        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> service().rebuildUserIndex(userId))
                .satisfies(ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE);
                    assertThat(ex.getCause()).isSameAs(failure);
                });
        verify(generationService, never()).activateGeneration(any(), any());
        verify(vectorStore, never()).delete(any(Filter.Expression.class));
    }

    @Test
    void rebuildUserIndex_addThrowsRuntimeException_doesNotActivateOrCleanup() {
        Long userId = 10L;
        List<NoteChunk> chunks = List.of(chunk("note:1:chunk:0", userId));
        List<Document> documents = List.of(document("note:1:chunk:0"));
        RuntimeException failure = new IllegalStateException("vector store failed");

        when(sourceService.loadChunks(userId)).thenReturn(chunks);
        when(documentMapper.toDocuments(chunks)).thenReturn(documents);
        when(generationService.getActiveGeneration(userId)).thenReturn("gen-old");
        doThrow(failure).when(vectorStore).add(anyList());

        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> service().rebuildUserIndex(userId))
                .satisfies(ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE);
                    assertThat(ex.getCause()).isSameAs(failure);
                });
        verify(generationService, never()).activateGeneration(any(), any());
        verify(vectorStore, never()).delete(any(Filter.Expression.class));
    }

    @Test
    void rebuildUserIndex_activateGenerationThrows_doesNotCleanup() {
        Long userId = 10L;
        List<NoteChunk> chunks = List.of(chunk("note:1:chunk:0", userId));
        List<Document> documents = List.of(document("note:1:chunk:0"));
        RuntimeException failure = new IllegalStateException("redis failed");

        when(sourceService.loadChunks(userId)).thenReturn(chunks);
        when(documentMapper.toDocuments(chunks)).thenReturn(documents);
        when(generationService.getActiveGeneration(userId)).thenReturn("gen-old");
        doThrow(failure).when(generationService).activateGeneration(userId, "gen-new");

        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> service("gen-new").rebuildUserIndex(userId))
                .satisfies(ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE);
                    assertThat(ex.getCause()).isSameAs(failure);
                });

        verify(vectorStore).add(anyList());
        verify(vectorStore, never()).delete(any(Filter.Expression.class));
    }

    @Test
    void rebuildUserIndex_cleanupOldGenerationThrows_stillReturnsSuccess() {
        Long userId = 10L;
        List<NoteChunk> chunks = List.of(chunk("note:1:chunk:0", userId));
        List<Document> documents = List.of(document("note:1:chunk:0"));

        when(sourceService.loadChunks(userId)).thenReturn(chunks);
        when(documentMapper.toDocuments(chunks)).thenReturn(documents);
        when(generationService.getActiveGeneration(userId)).thenReturn("gen-old");
        doThrow(new JedisException("cleanup failed"))
                .when(vectorStore).delete(oldGenerationFilter(userId, "gen-old"));

        AiIndexWriteResult result = service("gen-new").rebuildUserIndex(userId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.chunkCount()).isEqualTo(1);
        assertThat(result.indexedAt()).isNotNull();
        verify(generationService).activateGeneration(userId, "gen-new");
    }

    @Test
    void rebuildUserIndex_missingOldGenerationDoesNotCleanup() {
        Long userId = 10L;
        List<NoteChunk> chunks = List.of(chunk("note:1:chunk:0", userId));
        List<Document> documents = List.of(document("note:1:chunk:0"));

        when(sourceService.loadChunks(userId)).thenReturn(chunks);
        when(documentMapper.toDocuments(chunks)).thenReturn(documents);
        when(generationService.getActiveGeneration(userId)).thenReturn(null);

        AiIndexWriteResult result = service("gen-new").rebuildUserIndex(userId);

        assertThat(result.chunkCount()).isEqualTo(1);
        verify(vectorStore).add(anyList());
        verify(generationService).activateGeneration(userId, "gen-new");
        verify(vectorStore, never()).delete(any(Filter.Expression.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void rebuildUserIndex_addsNewGenerationActivatesAndCleansOldGeneration() {
        Long userId = 10L;
        List<NoteChunk> chunks = List.of(chunk("note:1:chunk:0", userId));
        List<Document> documents = List.of(document("note:1:chunk:0"));

        when(sourceService.loadChunks(userId)).thenReturn(chunks);
        when(documentMapper.toDocuments(chunks)).thenReturn(documents);
        when(generationService.getActiveGeneration(userId)).thenReturn("gen-old");

        AiIndexWriteResult result = service("gen-new").rebuildUserIndex(userId);

        InOrder inOrder = inOrder(sourceService, documentMapper, vectorStore, generationService);
        inOrder.verify(sourceService).loadChunks(userId);
        inOrder.verify(documentMapper).toDocuments(chunks);
        inOrder.verify(generationService).getActiveGeneration(userId);
        inOrder.verify(vectorStore).add(anyList());
        inOrder.verify(generationService).activateGeneration(userId, "gen-new");
        inOrder.verify(vectorStore).delete(oldGenerationFilter(userId, "gen-old"));

        ArgumentCaptor<List<Document>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> addedDocuments = documentsCaptor.getValue();
        assertThat(addedDocuments).hasSize(1);
        assertThat(addedDocuments.get(0).getMetadata())
                .containsEntry("userId", 10L)
                .containsEntry("indexGeneration", "gen-new");
        assertThat(documents.get(0).getMetadata()).doesNotContainKey("indexGeneration");

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.chunkCount()).isEqualTo(1);
        assertThat(result.indexedAt()).isNotNull();
    }

    @Test
    void rebuildUserIndex_emptyDocumentsActivatesNewGenerationAndCleansOldGeneration() {
        Long userId = 10L;
        List<NoteChunk> chunks = List.of();
        List<Document> documents = List.of();

        when(sourceService.loadChunks(userId)).thenReturn(chunks);
        when(documentMapper.toDocuments(chunks)).thenReturn(documents);
        when(generationService.getActiveGeneration(userId)).thenReturn("gen-old");

        AiIndexWriteResult result = service("gen-new").rebuildUserIndex(userId);

        InOrder inOrder = inOrder(sourceService, documentMapper, generationService, vectorStore);
        inOrder.verify(sourceService).loadChunks(userId);
        inOrder.verify(documentMapper).toDocuments(chunks);
        inOrder.verify(generationService).getActiveGeneration(userId);
        inOrder.verify(generationService).activateGeneration(userId, "gen-new");
        inOrder.verify(vectorStore).delete(oldGenerationFilter(userId, "gen-old"));
        verify(vectorStore, never()).add(anyList());

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.chunkCount()).isZero();
        assertThat(result.indexedAt()).isNotNull();
    }

    private AiIndexWriterService service() {
        return service("gen-new");
    }

    private AiIndexWriterService service(String generation) {
        return new AiIndexWriterService(vectorStore, sourceService, documentMapper, generationService, () -> generation);
    }

    private static Filter.Expression oldGenerationFilter(Long userId, String generation) {
        return new Filter.Expression(
                Filter.ExpressionType.AND,
                equalityFilter("userId", userId),
                equalityFilter("indexGeneration", generation)
        );
    }

    private static Filter.Expression equalityFilter(String key, Object value) {
        return new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key(key),
                new Filter.Value(value)
        );
    }

    private static NoteChunk chunk(String chunkId, Long userId) {
        return new NoteChunk(
                chunkId,
                userId,
                1L,
                0,
                "Title",
                "Text",
                "PRIVATE",
                LocalDateTime.of(2026, 6, 24, 10, 0),
                "hash"
        );
    }

    private static Document document(String id) {
        return Document.builder()
                .id(id)
                .text("Text")
                .metadata(Map.of("userId", 10L))
                .build();
    }
}
