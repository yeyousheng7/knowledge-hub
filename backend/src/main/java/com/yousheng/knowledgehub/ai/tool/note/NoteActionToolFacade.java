package com.yousheng.knowledgehub.ai.tool.note;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yousheng.knowledgehub.ai.agent.AiAgentSessionService;
import com.yousheng.knowledgehub.ai.agent.dto.ActionEnvelope;
import com.yousheng.knowledgehub.ai.agent.dto.AiAgentAction;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperation;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperationStore;
import com.yousheng.knowledgehub.ai.config.AiProperties;
import com.yousheng.knowledgehub.note.dto.NoteListItemResponse;
import com.yousheng.knowledgehub.note.dto.NoteListResponse;
import com.yousheng.knowledgehub.note.service.NoteService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NoteActionToolFacade {

    static final int MAX_BATCH_UNPUBLISH_NOTES = 20;
    static final String ACTION_TYPE_PENDING_OPERATION = "PENDING_OPERATION";
    static final String OPERATION_TYPE_BATCH_UNPUBLISH_NOTES = "BATCH_UNPUBLISH_NOTES";
    static final String STATUS_PENDING = "PENDING";

    private static final int DEFAULT_OPERATION_TTL_MINUTES = 30;

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

    public String prepareBatchUnpublishPublishedNotes() {
        Long userId = sessionService.requireCurrentEnabledUserId();
        NoteListResponse response = noteService.listMyPublishedNotes(1, MAX_BATCH_UNPUBLISH_NOTES);

        if (response.total() == 0) {
            return toJson(new ActionEnvelope(
                    "我没有找到需要批量下架的公开笔记。",
                    List.of()));
        }

        if (response.total() > MAX_BATCH_UNPUBLISH_NOTES) {
            return toJson(new ActionEnvelope(
                    "你当前公开笔记数量为 " + response.total() + " 篇，超过一次可准备的 "
                            + MAX_BATCH_UNPUBLISH_NOTES + " 篇上限。请先缩小范围后再操作。",
                    List.of()));
        }

        Instant createdAt = clock.instant();
        Duration ttl = operationTtl();
        Instant expiresAt = createdAt.plus(ttl);
        String operationId = UUID.randomUUID().toString();
        List<Long> noteIds = response.items().stream()
                .map(NoteListItemResponse::id)
                .toList();

        operationStore.save(new AiAgentPendingOperation(
                        operationId,
                        OPERATION_TYPE_BATCH_UNPUBLISH_NOTES,
                        userId,
                        noteIds,
                        createdAt,
                        expiresAt,
                        STATUS_PENDING),
                ttl);

        return toJson(new ActionEnvelope(
                "我找到了 " + response.total() + " 篇公开笔记，可以为你生成批量下架确认操作。请确认后再执行。",
                List.of(new AiAgentAction(ACTION_TYPE_PENDING_OPERATION, pendingActionPayload(
                        operationId,
                        response.items(),
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
}
