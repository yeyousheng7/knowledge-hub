package com.yousheng.knowledgehub.ai.agent;

import com.yousheng.knowledgehub.ai.agent.dto.AiAgentOperationAffectedItem;
import com.yousheng.knowledgehub.ai.agent.dto.AiAgentOperationConfirmResponse;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperation;
import com.yousheng.knowledgehub.ai.agent.operation.AiAgentPendingOperationStore;
import com.yousheng.knowledgehub.ai.tool.note.NoteActionToolFacade;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteBatchUnpublishResult;
import com.yousheng.knowledgehub.note.dto.NoteListItemResponse;
import com.yousheng.knowledgehub.note.service.NoteService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class AiAgentOperationConfirmService {

    private static final String STATUS_EXECUTED = "EXECUTED";

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

        validateOperation(userId, operation);

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

    private void validateOperation(Long userId, AiAgentPendingOperation operation) {
        if (!userId.equals(operation.userId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能确认其他用户的操作");
        }
        if (!NoteActionToolFacade.OPERATION_TYPE_BATCH_UNPUBLISH_NOTES.equals(operation.operationType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不支持的待确认操作类型");
        }
        if (!NoteActionToolFacade.STATUS_PENDING.equals(operation.status())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "待确认操作状态无效");
        }
        Instant expiresAt = operation.expiresAt();
        if (expiresAt == null || !expiresAt.isAfter(clock.instant())) {
            throw new BizException(ErrorCode.NOT_FOUND, "待确认操作不存在或已过期");
        }
        if (operation.noteIds() == null || operation.noteIds().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "待确认操作缺少笔记 ID");
        }
    }

    private AiAgentOperationAffectedItem toAffectedItem(NoteListItemResponse item) {
        return new AiAgentOperationAffectedItem(item.id(), item.title());
    }
}
