package com.yousheng.knowledgehub.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yousheng.knowledgehub.common.constant.SoftDeleteConstants;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.common.util.SqlLikeUtils;
import com.yousheng.knowledgehub.note.dto.*;
import com.yousheng.knowledgehub.note.entity.Note;
import com.yousheng.knowledgehub.note.enums.NoteModerationStatus;
import com.yousheng.knowledgehub.note.enums.NoteVisibility;
import com.yousheng.knowledgehub.note.mapper.NoteMapper;
import com.yousheng.knowledgehub.tag.dto.NoteTagQueryRow;
import com.yousheng.knowledgehub.tag.mapper.NoteTagMapper;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PublicNoteService {
    private final NoteMapper noteMapper;
    private final NoteTagMapper noteTagMapper;
    private final AppUserMapper appUserMapper;

    @Transactional(readOnly = true)
    public PublicNoteListResponse listPublicNotes(long page, long size, String keyword) {
        Page<Note> pageParam = Page.of(page, size);
        LambdaQueryWrapper<Note> query = basePublicNoteQuery()
                .orderByDesc(Note::getPublishedAt)
                .orderByDesc(Note::getId);

        String pattern = SqlLikeUtils.toContainsPattern(keyword);
        if (pattern != null) {
            query.and(wrapper -> wrapper
                    .apply("LOWER(title) LIKE {0} ESCAPE '!'", pattern)
                    .or()
                    .apply("LOWER(summary) LIKE {0} ESCAPE '!'", pattern)
                    .or()
                    .apply("LOWER(content_md) LIKE {0} ESCAPE '!'", pattern)
            );
        }

        Page<Note> notePage = noteMapper.selectPage(pageParam, query);
        return toPublicNoteListResponse(notePage);
    }

    @Transactional(readOnly = true)
    public PublicNoteListResponse listPublicNotesByAuthorId(Long authorId, long page, long size) {
        Page<Note> pageParam = Page.of(page, size);
        LambdaQueryWrapper<Note> query = basePublicNoteQuery()
                .eq(Note::getUserId, authorId)
                .orderByDesc(Note::getPublishedAt)
                .orderByDesc(Note::getId);

        Page<Note> notePage = noteMapper.selectPage(pageParam, query);
        return toPublicNoteListResponse(notePage);
    }

    @Transactional(readOnly = true)
    public PublicNoteDetailResponse getPublicNoteDetail(Long noteId) {
        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getVisibility, NoteVisibility.PUBLIC.name())
                .isNotNull(Note::getPublishedAt)
                .eq(Note::getModerationStatus, NoteModerationStatus.NORMAL.name())
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED);
        Note note = noteMapper.selectOne(query);
        if (note == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        return toPublicDetailResponse(note);
    }

    private LambdaQueryWrapper<Note> basePublicNoteQuery() {
        return Wrappers.lambdaQuery(Note.class)
                .eq(Note::getVisibility, NoteVisibility.PUBLIC.name())
                .eq(Note::getModerationStatus, NoteModerationStatus.NORMAL.name())
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED)
                .isNotNull(Note::getPublishedAt);
    }

    private PublicNoteListResponse toPublicNoteListResponse(Page<Note> notePage) {
        List<Note> notes = notePage.getRecords();
        Map<Long, List<PublicNoteTagResponse>> tagsByNoteId;
        Map<Long, PublicNoteAuthorResponse> authorByUserId;

        if (notes.isEmpty()) {
            tagsByNoteId = Map.of();
            authorByUserId = Map.of();
        } else {
            List<Long> authorUserIds = notes.stream()
                    .map(Note::getUserId)
                    .distinct()
                    .toList();

            authorByUserId = appUserMapper.selectList(
                            Wrappers.lambdaQuery(AppUser.class)
                                    .in(AppUser::getId, authorUserIds)
                    ).stream()
                    .collect(Collectors.toMap(
                            AppUser::getId,
                            row -> new PublicNoteAuthorResponse(row.getUsername(), row.getNickname())
                    ));

            List<Long> noteIds = notes.stream()
                    .map(Note::getId)
                    .toList();

            tagsByNoteId = noteTagMapper.selectTagRowsByNoteIds(noteIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            NoteTagQueryRow::noteId,
                            Collectors.mapping(
                                    row -> new PublicNoteTagResponse(row.tagName()),
                                    Collectors.toList()
                            )
                    ));
        }


        List<PublicNoteListItemResponse> items = notes.stream()
                .map(note -> {
                    PublicNoteAuthorResponse author = authorByUserId.getOrDefault(note.getUserId(), null);
                    List<PublicNoteTagResponse> tags = tagsByNoteId.getOrDefault(note.getId(), List.of());
                    return toPublicListItemResponse(note, tags, author);
                })
                .toList();

        return new PublicNoteListResponse(
                items,
                notePage.getTotal(),
                notePage.getCurrent(),
                notePage.getSize()
        );
    }

    private PublicNoteListItemResponse toPublicListItemResponse(Note note, List<PublicNoteTagResponse> tags, PublicNoteAuthorResponse author) {
        return new PublicNoteListItemResponse(
                note.getId(),
                note.getTitle(),
                note.getSummary(),
                tags,
                author,
                note.getPublishedAt(),
                note.getUpdatedAt()
        );
    }

    private PublicNoteDetailResponse toPublicDetailResponse(Note note) {
        List<PublicNoteTagResponse> tags =
                noteTagMapper.selectTagResponseByNoteId(note.getId())
                        .stream()
                        .map(noteTagResponse -> new PublicNoteTagResponse(noteTagResponse.name()))
                        .toList();

        PublicNoteAuthorResponse author = toPublicAuthorResponse(note.getUserId());

        return new PublicNoteDetailResponse(
                note.getId(),
                note.getTitle(),
                note.getContentMd(),
                note.getSummary(),
                tags,
                author,
                note.getPublishedAt(),
                note.getUpdatedAt()
        );
    }

    private PublicNoteAuthorResponse toPublicAuthorResponse(Long userId) {
        AppUser author = appUserMapper.selectById(userId);
        if (author == null) {
            return null;
        }

        return new PublicNoteAuthorResponse(
                author.getUsername(),
                author.getNickname()
        );
    }
}
