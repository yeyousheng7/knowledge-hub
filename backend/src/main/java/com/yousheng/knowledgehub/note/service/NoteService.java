package com.yousheng.knowledgehub.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.*;
import com.yousheng.knowledgehub.note.entity.Note;
import com.yousheng.knowledgehub.note.enums.NoteModerationStatus;
import com.yousheng.knowledgehub.note.enums.NoteVisibility;
import com.yousheng.knowledgehub.note.mapper.NoteMapper;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class NoteService {
    private final AppUserMapper appUserMapper;
    private final NoteMapper noteMapper;
    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;

    @Transactional
    public NoteCreateResponse createNote(NoteCreateRequest request) {
        Long userId = requireCurrentEnabledUserId();

        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(request.title().trim());
        note.setContentMd(request.contentMd());
        note.setSummary(request.summary());
        note.setVisibility(NoteVisibility.PRIVATE.name());
        note.setModerationStatus(NoteModerationStatus.NORMAL.name());
        note.setDeleted(NOT_DELETED);

        noteMapper.insert(note);

        return new NoteCreateResponse(
                note.getId(),
                note.getTitle(),
                note.getVisibility(),
                note.getModerationStatus(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public NoteDetailResponse getMyNoteDetail(Long noteId) {
        Long userId = requireCurrentEnabledUserId();

        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, NOT_DELETED);

        Note note = noteMapper.selectOne(query);

        if (note == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        return new NoteDetailResponse(
                note.getId(),
                note.getTitle(),
                note.getContentMd(),
                note.getSummary(),
                note.getVisibility(),
                note.getModerationStatus(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.getPublishedAt()
        );

    }

    @Transactional(readOnly = true)
    public NoteListResponse listMyNotes(long page, long size) {
        Long userId = requireCurrentEnabledUserId();
        Page<Note> pageParam = Page.of(page, size);
        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, NOT_DELETED)
                .orderByDesc(Note::getUpdatedAt)
                .orderByDesc(Note::getId);

        Page<Note> notePage = noteMapper.selectPage(pageParam, query);

        List<NoteListItemResponse> items = notePage.getRecords()
                .stream()
                .map(this::toListItemResponse)
                .toList();

        return new NoteListResponse(
                items,
                notePage.getTotal(),
                notePage.getCurrent(),
                notePage.getSize()
        );
    }

    @Transactional
    public NoteDetailResponse updateNote(Long noteId, NoteUpdateRequest request) {
        Long userId = requireCurrentEnabledUserId();
        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, NOT_DELETED);

        Note note = noteMapper.selectOne(query);
        if (note == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        note.setTitle(request.title().trim());
        note.setContentMd(request.contentMd());
        note.setSummary(request.summary());

        noteMapper.updateById(note);

        return new NoteDetailResponse(
                note.getId(),
                note.getTitle(),
                note.getContentMd(),
                note.getSummary(),
                note.getVisibility(),
                note.getModerationStatus(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.getPublishedAt()
        );
    }

    @Transactional
    public void deleteNote(Long noteId) {
        Long userId = requireCurrentEnabledUserId();
        LocalDateTime now = LocalDateTime.now();

        Note updateNote = new Note();
        updateNote.setDeleted(DELETED);
        updateNote.setDeletedAt(now);

        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, NOT_DELETED);

        int affectedRows = noteMapper.update(updateNote, query);
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }
    }

    private NoteListItemResponse toListItemResponse(Note note) {
        return new NoteListItemResponse(
                note.getId(),
                note.getTitle(),
                note.getSummary(),
                note.getVisibility(),
                note.getModerationStatus(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.getPublishedAt()
        );
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
