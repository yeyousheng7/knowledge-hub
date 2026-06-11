package com.yousheng.knowledgehub.admin.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yousheng.knowledgehub.admin.dto.AdminNoteModerationResponse;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.entity.Note;
import com.yousheng.knowledgehub.note.enums.NoteModerationStatus;
import com.yousheng.knowledgehub.note.enums.NoteVisibility;
import com.yousheng.knowledgehub.note.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class AdminNoteService {
    private final NoteMapper noteMapper;
    private static final int NOT_DELETED = 0;

    @Transactional
    public AdminNoteModerationResponse takeDownNote(Long noteId) {
        Note note = noteMapper.selectOne(
                Wrappers.lambdaQuery(Note.class)
                        .eq(Note::getId, noteId)
                        .eq(Note::getVisibility, NoteVisibility.PUBLIC.name())
                        .eq(Note::getDeleted, NOT_DELETED)
        );
        if (note == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        if (NoteModerationStatus.TAKEN_DOWN.name().equals(note.getModerationStatus())) {
            return new AdminNoteModerationResponse(
                    note.getId(),
                    note.getModerationStatus(),
                    note.getModeratedAt()
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Note> updateWrapper = Wrappers.lambdaUpdate(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getModerationStatus, NoteModerationStatus.NORMAL.name())
                .eq(Note::getVisibility, NoteVisibility.PUBLIC.name())
                .eq(Note::getDeleted, NOT_DELETED)
                .set(Note::getModerationStatus, NoteModerationStatus.TAKEN_DOWN.name())
                .set(Note::getModeratedAt, now);

        // updatedAt 依赖 MP 自动更新, 不要删除 new Note()
        int affectedRows = noteMapper.update(new Note(), updateWrapper);

        if (affectedRows == 0) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        return new AdminNoteModerationResponse(
                noteId,
                NoteModerationStatus.TAKEN_DOWN.name(),
                now
        );
    }

}
