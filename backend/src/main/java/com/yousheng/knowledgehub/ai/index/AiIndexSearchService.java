package com.yousheng.knowledgehub.ai.index;

import com.yousheng.knowledgehub.ai.config.AiProperties;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class AiIndexSearchService {

    private static final Logger log = LoggerFactory.getLogger(AiIndexSearchService.class);
    private static final String INDEX_GENERATION_METADATA_KEY = "indexGeneration";

    private final VectorStore vectorStore;
    private final AiIndexGenerationService generationService;
    private final AiProperties aiProperties;
    private final AppUserMapper appUserMapper;

    public AiIndexSearchResult search(String query) {
        Long currentUserId = requireCurrentEnabledUserId();

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("query must not be blank");
        }

        String trimmedQuery = query.trim();

        String activeGeneration = getActiveGeneration(currentUserId);
        if (activeGeneration == null) {
            return new AiIndexSearchResult(currentUserId, trimmedQuery, null, Collections.emptyList());
        }

        Filter.Expression filter = searchFilter(currentUserId, activeGeneration);
        int topK = aiProperties.getIndex().getTopK();
        SearchRequest searchRequest = SearchRequest.builder()
                .query(trimmedQuery)
                .topK(topK)
                .filterExpression(filter)
                .build();

        List<Document> documents = similaritySearch(currentUserId, searchRequest);

        List<AiIndexSearchHit> hits = documents.stream()
                .map(this::toHit)
                .toList();

        return new AiIndexSearchResult(currentUserId, trimmedQuery, activeGeneration, hits);
    }

    private String getActiveGeneration(Long userId) {
        try {
            return generationService.getActiveGeneration(userId);
        } catch (RuntimeException ex) {
            log.warn("AI index search failed, userId={}", userId, ex);
            throw new BizException(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE, ex);
        }
    }

    private List<Document> similaritySearch(Long userId, SearchRequest searchRequest) {
        try {
            return vectorStore.similaritySearch(searchRequest);
        } catch (RuntimeException ex) {
            log.warn("AI index search failed, userId={}", userId, ex);
            throw new BizException(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE, ex);
        }
    }

    private AiIndexSearchHit toHit(Document document) {
        var metadata = document.getMetadata();

        Long noteId = toLong(metadata.get("noteId"));
        String title = (String) metadata.get("title");
        String chunkText = document.getText();
        int chunkIndex = toInt(metadata.get("chunkIndex"), 0);
        Double distance = toDouble(metadata.get("distance"));
        if (distance == null) {
            distance = toDouble(metadata.get("score"));
        }
        String visibility = (String) metadata.get("visibility");
        LocalDateTime updatedAt = toLocalDateTime(metadata.get("updatedAt"));

        return new AiIndexSearchHit(noteId, title, chunkText, chunkIndex, distance, visibility, updatedAt);
    }

    private static Filter.Expression searchFilter(Long userId, String generation) {
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

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number num) return num.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime ldt) return ldt;
        try {
            return LocalDateTime.parse(value.toString());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Long requireCurrentEnabledUserId() {
        Long userId = CurrentUser.getUserId();
        AppUser user = appUserMapper.selectById(userId);

        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        return userId;
    }
}
