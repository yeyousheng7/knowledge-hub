package com.yousheng.knowledgehub.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yousheng.knowledgehub.admin.dto.AdminNoteAuthorResponse;
import com.yousheng.knowledgehub.admin.dto.AdminNoteItemResponse;
import com.yousheng.knowledgehub.admin.dto.AdminNoteListResponse;
import com.yousheng.knowledgehub.admin.dto.AdminNoteModerationResponse;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.entity.Note;
import com.yousheng.knowledgehub.note.enums.NoteModerationStatus;
import com.yousheng.knowledgehub.note.enums.NoteVisibility;
import com.yousheng.knowledgehub.note.mapper.NoteMapper;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AdminNoteService {
    private final AdminPermissionService adminPermissionService;

    private final NoteMapper noteMapper;

    private static final int NOT_DELETED = 0;
    private final AppUserMapper appUserMapper;


    @Transactional
    public AdminNoteModerationResponse takeDownNote(Long noteId) {
        adminPermissionService.requireCurrentAdminEnabled();

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

    @Transactional
    public AdminNoteModerationResponse restoreNote(Long noteId) {
        adminPermissionService.requireCurrentAdminEnabled();

        Note note = noteMapper.selectOne(
                Wrappers.lambdaQuery(Note.class)
                        .eq(Note::getId, noteId)
                        .eq(Note::getVisibility, NoteVisibility.PUBLIC.name())
                        .eq(Note::getDeleted, NOT_DELETED)
        );

        if (note == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        if (NoteModerationStatus.NORMAL.name().equals(note.getModerationStatus())) {
            return new AdminNoteModerationResponse(
                    noteId,
                    NoteModerationStatus.NORMAL.name(),
                    note.getModeratedAt()
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Note> updateWrapper = Wrappers.lambdaUpdate(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getDeleted, NOT_DELETED)
                .eq(Note::getVisibility, NoteVisibility.PUBLIC.name())
                .eq(Note::getModerationStatus, NoteModerationStatus.TAKEN_DOWN.name())
                .set(Note::getModerationStatus, NoteModerationStatus.NORMAL.name())
                .set(Note::getModeratedAt, now);

        int affectedRows = noteMapper.update(new Note(), updateWrapper);
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        return new AdminNoteModerationResponse(
                noteId,
                NoteModerationStatus.NORMAL.name(),
                now
        );
    }

    @Transactional(readOnly = true)
    public AdminNoteListResponse listPublicNotesForAdmin(long page, long size, String keyword, String moderationStatus) {
        adminPermissionService.requireCurrentAdminEnabled();
        Page<Note> pageParam = Page.of(page, size);

        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .orderByDesc(Note::getUpdatedAt)
                .orderByDesc(Note::getId);

        query.eq(Note::getVisibility, NoteVisibility.PUBLIC.name())
                .eq(Note::getDeleted, NOT_DELETED);

        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
            String pattern = "%" + normalizedKeyword + "%";

            query.and(wrapper -> wrapper
                    .apply("LOWER(title) LIKE {0} ESCAPE '!'", pattern)
                    .or()
                    .apply("LOWER(summary) LIKE {0} ESCAPE '!'", pattern)
            );
        }

        String status = moderationStatus == null ? "" : moderationStatus.trim();
        if (!status.isBlank()) {
            query.eq(Note::getModerationStatus, status);
        }

        Page<Note> notePage = noteMapper.selectPage(pageParam, query);

        if (notePage == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        if (notePage.getRecords().isEmpty()) {
            return new AdminNoteListResponse(
                    List.of(),
                    notePage.getTotal(),
                    notePage.getCurrent(),
                    notePage.getSize()
            );
        }

        List<Long> userIds = notePage.getRecords()
                .stream()
                .map(Note::getUserId)
                .distinct()
                .toList();

        Map<Long, AdminNoteAuthorResponse> authorByUserId = appUserMapper.selectByIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        AppUser::getId,
                        user -> new AdminNoteAuthorResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getNickname(),
                                user.getStatus()
                        )
                ));


        List<AdminNoteItemResponse> items = notePage.getRecords()
                .stream()
                .map(note -> new AdminNoteItemResponse(
                        note.getId(),
                        note.getTitle(),
                        note.getSummary(),
                        authorByUserId.getOrDefault(note.getUserId(), null),
                        note.getVisibility(),
                        note.getModerationStatus(),
                        note.getPublishedAt(),
                        note.getModeratedAt(),
                        note.getUpdatedAt()
                ))
                .toList();

        return new AdminNoteListResponse(
                items,
                notePage.getTotal(),
                notePage.getCurrent(),
                notePage.getSize()
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);

        // 替换 Like 特殊符号
        if (!normalizedKeyword.isEmpty()) {
            normalizedKeyword = normalizedKeyword
                    .replace("!", "!!")
                    .replace("%", "!%")
                    .replace("_", "!_");
        }
        return normalizedKeyword;
    }
}
