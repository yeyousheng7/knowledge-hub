package com.yousheng.knowledgehub.ai.index;

import com.yousheng.knowledgehub.ai.index.model.NoteChunk;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import redis.clients.jedis.exceptions.JedisException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class AiIndexWriterService {

    private static final Logger log = LoggerFactory.getLogger(AiIndexWriterService.class);
    private static final String INDEX_GENERATION_METADATA_KEY = "indexGeneration";

    private final VectorStore vectorStore;
    private final AiNoteIndexSourceService sourceService;
    private final NoteChunkDocumentMapper documentMapper;
    private final AiIndexGenerationService generationService;
    private final Supplier<String> generationSupplier;

    public AiIndexWriterService(
            VectorStore vectorStore,
            AiNoteIndexSourceService sourceService,
            NoteChunkDocumentMapper documentMapper,
            AiIndexGenerationService generationService) {
        this(vectorStore, sourceService, documentMapper, generationService, () -> UUID.randomUUID().toString());
    }

    AiIndexWriterService(
            VectorStore vectorStore,
            AiNoteIndexSourceService sourceService,
            NoteChunkDocumentMapper documentMapper,
            AiIndexGenerationService generationService,
            Supplier<String> generationSupplier) {
        this.vectorStore = vectorStore;
        this.sourceService = sourceService;
        this.documentMapper = documentMapper;
        this.generationService = generationService;
        this.generationSupplier = generationSupplier;
    }

    public AiIndexWriteResult rebuildUserIndex(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        List<NoteChunk> chunks = sourceService.loadChunks(userId);
        List<Document> documents = documentMapper.toDocuments(chunks);
        String newGeneration = newGeneration();
        String oldGeneration = getActiveGeneration(userId);
        List<Document> generationDocuments = withIndexGeneration(documents, newGeneration);

        addGenerationDocuments(userId, generationDocuments);
        activateGeneration(userId, newGeneration);
        cleanupOldGeneration(userId, oldGeneration);

        return new AiIndexWriteResult(userId, documents.size(), Instant.now());
    }

    private String newGeneration() {
        String generation = generationSupplier.get();
        if (generation == null || generation.isBlank()) {
            throw new IllegalStateException("index generation must not be blank");
        }
        return generation;
    }

    private String getActiveGeneration(Long userId) {
        try {
            return generationService.getActiveGeneration(userId);
        } catch (RuntimeException ex) {
            log.warn("AI index write failed, userId={}", userId, ex);
            throw new BizException(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE, ex);
        }
    }

    private void addGenerationDocuments(Long userId, List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }

        try {
            vectorStore.add(documents);
        } catch (JedisException ex) {
            log.warn("AI index write failed, userId={}", userId, ex);
            throw new BizException(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE, ex);
        } catch (RuntimeException ex) {
            log.warn("AI index write failed, userId={}", userId, ex);
            throw new BizException(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE, ex);
        }
    }

    private void activateGeneration(Long userId, String generation) {
        try {
            generationService.activateGeneration(userId, generation);
        } catch (RuntimeException ex) {
            log.warn("AI index write failed, userId={}", userId, ex);
            throw new BizException(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE, ex);
        }
    }

    private void cleanupOldGeneration(Long userId, String oldGeneration) {
        if (oldGeneration == null || oldGeneration.isBlank()) {
            return;
        }

        try {
            vectorStore.delete(oldGenerationFilter(userId, oldGeneration));
        } catch (JedisException ex) {
            log.warn("AI index old generation cleanup failed, userId={}, generation={}", userId, oldGeneration, ex);
        } catch (RuntimeException ex) {
            log.warn("AI index old generation cleanup failed, userId={}, generation={}", userId, oldGeneration, ex);
        }
    }

    private static List<Document> withIndexGeneration(List<Document> documents, String generation) {
        return documents.stream()
                .map(document -> withIndexGeneration(document, generation))
                .toList();
    }

    private static Document withIndexGeneration(Document document, String generation) {
        Map<String, Object> metadata = new HashMap<>(document.getMetadata());
        metadata.put(INDEX_GENERATION_METADATA_KEY, generation);
        return document.mutate()
                .metadata(metadata)
                .build();
    }

    private static Filter.Expression oldGenerationFilter(Long userId, String generation) {
        return new Filter.Expression(
                Filter.ExpressionType.AND,
                equalityFilter("userId", userId),
                equalityFilter(INDEX_GENERATION_METADATA_KEY, generation)
        );
    }

    private static Filter.Expression equalityFilter(String key, Object value) {
        return new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key(key),
                new Filter.Value(value)
        );
    }
}
