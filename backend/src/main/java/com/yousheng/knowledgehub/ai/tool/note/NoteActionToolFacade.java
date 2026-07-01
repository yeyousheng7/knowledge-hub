package com.yousheng.knowledgehub.ai.tool.note;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yousheng.knowledgehub.ai.agent.AiAgentSessionService;
import com.yousheng.knowledgehub.ai.agent.dto.ActionEnvelope;
import com.yousheng.knowledgehub.ai.agent.dto.AiAgentAction;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperation;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperationStore;
import com.yousheng.knowledgehub.ai.config.AiProperties;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteListItemResponse;
import com.yousheng.knowledgehub.note.service.NoteService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NoteActionToolFacade {

    public static final String ACTION_TYPE_PENDING_OPERATION = "PENDING_OPERATION";
    public static final String OPERATION_TYPE_BATCH_UNPUBLISH_NOTES = "BATCH_UNPUBLISH_NOTES";
    public static final String OPERATION_TYPE_CREATE_PRIVATE_NOTE = "CREATE_PRIVATE_NOTE";
    public static final String STATUS_PENDING = "PENDING";

    private static final int DEFAULT_OPERATION_TTL_MINUTES = 30;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_CONTENT_MD_LENGTH = 100_000;
    private static final int MAX_SUMMARY_LENGTH = 300;
    private static final int MAX_RECOMMENDED_TAGS = 10;

    private final NoteService noteService;
    private final AiAgentSessionService sessionService;
    private final AiAgentPendingOperationStore operationStore;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NoteActionToolFacade(NoteService noteService,
                                AiAgentSessionService sessionService,
                                AiAgentPendingOperationStore operationStore,
                                AiProperties aiProperties,
                                ObjectMapper objectMapper) {
        this(noteService, sessionService, operationStore, aiProperties, objectMapper, Clock.systemUTC());
    }

    NoteActionToolFacade(NoteService noteService,
                         AiAgentSessionService sessionService,
                         AiAgentPendingOperationStore operationStore,
                         AiProperties aiProperties,
                         ObjectMapper objectMapper,
                         Clock clock) {
        this.noteService = noteService;
        this.sessionService = sessionService;
        this.operationStore = operationStore;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String prepareBatchUnpublishPublishedNotes(List<Long> noteIds) {
        Long userId = sessionService.requireCurrentEnabledUserId();
        List<NoteListItemResponse> notes;
        try {
            notes = noteService.getMyPublishedNotesForBatchUnpublish(noteIds);
        } catch (BizException e) {
            if (e.getErrorCode() == ErrorCode.BAD_REQUEST || e.getErrorCode() == ErrorCode.NOTE_NOT_FOUND) {
                return toJson(new ActionEnvelope(e.getMessage(), List.of()));
            }
            throw e;
        }

        Instant createdAt = clock.instant();
        Duration ttl = operationTtl();
        Instant expiresAt = createdAt.plus(ttl);
        String operationId = UUID.randomUUID().toString();
        List<Long> validatedNoteIds = notes.stream()
                .map(NoteListItemResponse::id)
                .toList();

        operationStore.save(new AiAgentPendingOperation(
                        operationId,
                        OPERATION_TYPE_BATCH_UNPUBLISH_NOTES,
                        userId,
                        validatedNoteIds,
                        createdAt,
                        expiresAt,
                        STATUS_PENDING),
                ttl);

        return toJson(new ActionEnvelope(
                "已选择 " + notes.size() + " 篇公开笔记，可以为你生成批量下架确认操作。请确认后再执行。",
                List.of(new AiAgentAction(ACTION_TYPE_PENDING_OPERATION, pendingActionPayload(
                        operationId,
                        notes,
                        expiresAt)))));
    }

    public String prepareCreatePrivateNote(String title,
                                           String contentMd,
                                           String summary,
                                           List<String> recommendedTags) {
        Long userId = sessionService.requireCurrentEnabledUserId();
        Draft draft = normalizeDraft(title, contentMd, summary, recommendedTags);
        if (!draft.valid()) {
            return toJson(new ActionEnvelope(draft.errorMessage(), List.of()));
        }

        Instant createdAt = clock.instant();
        Duration ttl = operationTtl();
        Instant expiresAt = createdAt.plus(ttl);
        String operationId = UUID.randomUUID().toString();
        Map<String, Object> draftPayload = draftPayload(draft);

        operationStore.save(new AiAgentPendingOperation(
                        operationId,
                        OPERATION_TYPE_CREATE_PRIVATE_NOTE,
                        userId,
                        List.of(),
                        draftPayload,
                        createdAt,
                        expiresAt,
                        STATUS_PENDING),
                ttl);

        return toJson(new ActionEnvelope(
                "我已经整理好私有笔记草稿。请确认后再创建笔记。",
                List.of(new AiAgentAction(ACTION_TYPE_PENDING_OPERATION, createPrivateNoteActionPayload(
                        operationId,
                        draftPayload,
                        expiresAt)))));
    }

    private Duration operationTtl() {
        int ttlMinutes = aiProperties.getAgent().getOperation().getTtlMinutes();
        if (ttlMinutes <= 0) {
            ttlMinutes = DEFAULT_OPERATION_TTL_MINUTES;
        }
        return Duration.ofMinutes(ttlMinutes);
    }

    private Map<String, Object> pendingActionPayload(String operationId,
                                                     List<NoteListItemResponse> notes,
                                                     Instant expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operationId", operationId);
        payload.put("operationType", OPERATION_TYPE_BATCH_UNPUBLISH_NOTES);
        payload.put("preview", "准备批量下架 " + notes.size() + " 篇公开笔记");
        payload.put("affectedItems", notes.stream()
                .map(this::affectedItem)
                .toList());
        payload.put("expiresAt", expiresAt.toString());
        return payload;
    }

    private Map<String, Object> createPrivateNoteActionPayload(String operationId,
                                                               Map<String, Object> draftPayload,
                                                               Instant expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operationId", operationId);
        payload.put("operationType", OPERATION_TYPE_CREATE_PRIVATE_NOTE);
        payload.put("preview", "准备创建私有笔记：" + draftPayload.get("title"));
        payload.put("draft", draftPayload);
        payload.put("expiresAt", expiresAt.toString());
        return payload;
    }

    private Map<String, Object> draftPayload(Draft draft) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", draft.title());
        payload.put("summary", draft.summary());
        payload.put("contentMd", draft.contentMd());
        payload.put("recommendedTags", draft.recommendedTags());
        return payload;
    }

    private Map<String, Object> affectedItem(NoteListItemResponse note) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", note.id());
        item.put("title", note.title());
        return item;
    }

    private String toJson(ActionEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AI agent action envelope", e);
        }
    }

    private Draft normalizeDraft(String title, String contentMd, String summary, List<String> recommendedTags) {
        String normalizedTitle = trimToNull(title);
        if (normalizedTitle == null) {
            return Draft.invalid("笔记标题不能为空，请补充标题后再创建草稿。");
        }
        if (normalizedTitle.length() > MAX_TITLE_LENGTH) {
            return Draft.invalid("笔记标题不能超过 " + MAX_TITLE_LENGTH + " 个字符。");
        }

        String normalizedContent = trimToNull(contentMd);
        if (normalizedContent == null) {
            return Draft.invalid("笔记正文不能为空，请补充正文后再创建草稿。");
        }
        if (normalizedContent.length() > MAX_CONTENT_MD_LENGTH) {
            return Draft.invalid("笔记正文不能超过 " + MAX_CONTENT_MD_LENGTH + " 个字符。");
        }

        String normalizedSummary = trimToNull(summary);
        if (normalizedSummary != null && normalizedSummary.length() > MAX_SUMMARY_LENGTH) {
            return Draft.invalid("笔记摘要不能超过 " + MAX_SUMMARY_LENGTH + " 个字符。");
        }

        return Draft.valid(
                normalizedTitle,
                normalizedContent,
                normalizedSummary,
                normalizeRecommendedTags(recommendedTags));
    }

    private List<String> normalizeRecommendedTags(List<String> recommendedTags) {
        if (recommendedTags == null || recommendedTags.isEmpty()) {
            return List.of();
        }
        return recommendedTags.stream()
                .map(this::trimToNull)
                .filter(tag -> tag != null && tag.length() <= 50)
                .distinct()
                .limit(MAX_RECOMMENDED_TAGS)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Draft(
            boolean valid,
            String title,
            String contentMd,
            String summary,
            List<String> recommendedTags,
            String errorMessage
    ) {
        static Draft valid(String title, String contentMd, String summary, List<String> recommendedTags) {
            return new Draft(true, title, contentMd, summary, recommendedTags, null);
        }

        static Draft invalid(String errorMessage) {
            return new Draft(false, null, null, null, List.of(), errorMessage);
        }
    }
}
