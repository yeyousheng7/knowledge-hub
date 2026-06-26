package com.yousheng.knowledgehub.ai.agent;

import com.yousheng.knowledgehub.ai.agent.dto.AiAgentOperationAffectedItem;
import com.yousheng.knowledgehub.ai.agent.dto.AiAgentOperationConfirmResponse;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperation;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperationStore;
import com.yousheng.knowledgehub.ai.tool.note.NoteActionToolFacade;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteBatchUnpublishResult;
import com.yousheng.knowledgehub.note.dto.NoteCreateRequest;
import com.yousheng.knowledgehub.note.dto.NoteCreateResponse;
import com.yousheng.knowledgehub.note.dto.NoteListItemResponse;
import com.yousheng.knowledgehub.note.service.NoteService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AiAgentOperationConfirmService {

    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_CONTENT_MD_LENGTH = 100_000;
    private static final int MAX_SUMMARY_LENGTH = 300;

    private final AiAgentSessionService sessionService;
    private final AiAgentPendingOperationStore operationStore;
    private final NoteService noteService;
    private final Clock clock;

    public AiAgentOperationConfirmService(AiAgentSessionService sessionService,
                                          AiAgentPendingOperationStore operationStore,
                                          NoteService noteService) {
        this(sessionService, operationStore, noteService, Clock.systemUTC());
    }

    AiAgentOperationConfirmService(AiAgentSessionService sessionService,
                                   AiAgentPendingOperationStore operationStore,
                                   NoteService noteService,
                                   Clock clock) {
        this.sessionService = sessionService;
        this.operationStore = operationStore;
        this.noteService = noteService;
        this.clock = clock;
    }

    public AiAgentOperationConfirmResponse confirm(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "operationId 不能为空");
        }

        Long userId = sessionService.requireCurrentEnabledUserId();
        AiAgentPendingOperation operation = operationStore.consume(userId, operationId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "待确认操作不存在或已过期"));

        validateCommonOperation(userId, operation);
        if (NoteActionToolFacade.OPERATION_TYPE_BATCH_UNPUBLISH_NOTES.equals(operation.operationType())) {
            return confirmBatchUnpublish(operation);
        }
        if (NoteActionToolFacade.OPERATION_TYPE_CREATE_PRIVATE_NOTE.equals(operation.operationType())) {
            return confirmCreatePrivateNote(operation);
        }

        throw new BizException(ErrorCode.BAD_REQUEST, "不支持的待确认操作类型");
    }

    private AiAgentOperationConfirmResponse confirmBatchUnpublish(AiAgentPendingOperation operation) {
        validateBatchUnpublishOperation(operation);
        NoteBatchUnpublishResult result = noteService.batchUnpublishMyPublishedNotes(operation.noteIds());
        return new AiAgentOperationConfirmResponse(
                operation.operationId(),
                operation.operationType(),
                STATUS_EXECUTED,
                result.affectedCount(),
                result.affectedItems().stream()
                        .map(this::toAffectedItem)
                        .toList(),
                "已下架 " + result.affectedCount() + " 篇公开笔记。");
    }

    private AiAgentOperationConfirmResponse confirmCreatePrivateNote(AiAgentPendingOperation operation) {
        Draft draft = requireValidDraft(operation.payload());
        NoteCreateResponse createdNote = noteService.createNote(new NoteCreateRequest(
                draft.title(),
                draft.contentMd(),
                draft.summary(),
                null,
                List.of()));

        return new AiAgentOperationConfirmResponse(
                operation.operationId(),
                operation.operationType(),
                STATUS_EXECUTED,
                1,
                List.of(new AiAgentOperationAffectedItem(createdNote.id(), createdNote.title())),
                "已创建私有笔记：" + createdNote.title());
    }

    private void validateCommonOperation(Long userId, AiAgentPendingOperation operation) {
        if (!userId.equals(operation.userId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能确认其他用户的操作");
        }
        if (!NoteActionToolFacade.STATUS_PENDING.equals(operation.status())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "待确认操作状态无效");
        }
        Instant expiresAt = operation.expiresAt();
        if (expiresAt == null || !expiresAt.isAfter(clock.instant())) {
            throw new BizException(ErrorCode.NOT_FOUND, "待确认操作不存在或已过期");
        }
    }

    private void validateBatchUnpublishOperation(AiAgentPendingOperation operation) {
        if (operation.noteIds() == null || operation.noteIds().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "待确认操作缺少笔记 ID");
        }
    }

    private AiAgentOperationAffectedItem toAffectedItem(NoteListItemResponse item) {
        return new AiAgentOperationAffectedItem(item.id(), item.title());
    }

    private Draft requireValidDraft(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "待确认操作缺少笔记草稿");
        }

        String title = requireString(payload, "title", "笔记标题不能为空");
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new BizException(ErrorCode.BAD_REQUEST, "笔记标题不能超过 " + MAX_TITLE_LENGTH + " 个字符");
        }

        String contentMd = requireString(payload, "contentMd", "笔记正文不能为空");
        if (contentMd.length() > MAX_CONTENT_MD_LENGTH) {
            throw new BizException(ErrorCode.BAD_REQUEST, "笔记正文不能超过 " + MAX_CONTENT_MD_LENGTH + " 个字符");
        }

        String summary = optionalString(payload, "summary");
        if (summary != null && summary.length() > MAX_SUMMARY_LENGTH) {
            throw new BizException(ErrorCode.BAD_REQUEST, "笔记摘要不能超过 " + MAX_SUMMARY_LENGTH + " 个字符");
        }

        return new Draft(title, contentMd, summary);
    }

    private String requireString(Map<String, Object> payload, String key, String errorMessage) {
        String value = optionalString(payload, key);
        if (value == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, errorMessage);
        }
        return value;
    }

    private String optionalString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof String stringValue)) {
            return null;
        }
        String trimmed = stringValue.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Draft(String title, String contentMd, String summary) {
    }
}
