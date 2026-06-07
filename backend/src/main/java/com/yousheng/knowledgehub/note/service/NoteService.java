package com.yousheng.knowledgehub.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yousheng.knowledgehub.category.entity.Category;
import com.yousheng.knowledgehub.category.mapper.CategoryMapper;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.*;
import com.yousheng.knowledgehub.note.entity.Note;
import com.yousheng.knowledgehub.note.enums.NoteModerationStatus;
import com.yousheng.knowledgehub.note.enums.NoteVisibility;
import com.yousheng.knowledgehub.note.mapper.NoteMapper;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.tag.entity.NoteTag;
import com.yousheng.knowledgehub.tag.entity.Tag;
import com.yousheng.knowledgehub.tag.mapper.NoteTagMapper;
import com.yousheng.knowledgehub.tag.mapper.TagMapper;
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
    private final CategoryMapper categoryMapper;
    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;
    private final NoteTagMapper noteTagMapper;
    private final TagMapper tagMapper;

    @Transactional
    public NoteCreateResponse createNote(NoteCreateRequest request) {
        Long userId = requireCurrentEnabledUserId();
        validateCategoryBelongsToCurrentUser(userId, request.categoryId());
        List<Long> tagIds = normalizeAndValidateTagIds(userId, request.tagIds());

        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(request.title().trim());
        note.setContentMd(request.contentMd());
        note.setSummary(request.summary());
        note.setCategoryId(request.categoryId());
        note.setVisibility(NoteVisibility.PRIVATE.name());
        note.setModerationStatus(NoteModerationStatus.NORMAL.name());
        note.setDeleted(NOT_DELETED);

        noteMapper.insert(note);

        // 设置 note-tag 关系
        if (!tagIds.isEmpty()) {
            bindTags(note.getId(), tagIds);
        }

        List<NoteTagResponse> tags = noteTagMapper.selectTagResponseByNoteId(note.getId());

        return new NoteCreateResponse(
                note.getId(),
                note.getTitle(),
                note.getVisibility(),
                note.getModerationStatus(),
                note.getCategoryId(),
                tags,
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

        return toDetailResponse(note);

    }

    @Transactional(readOnly = true)
    public NoteListResponse listMyNotes(long page, long size, Long categoryId) {
        Long userId = requireCurrentEnabledUserId();
        Page<Note> pageParam = Page.of(page, size);
        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, NOT_DELETED)
                .orderByDesc(Note::getUpdatedAt)
                .orderByDesc(Note::getId);

        if (categoryId != null) {
            validateCategoryBelongsToCurrentUser(userId, categoryId);
            query.eq(Note::getCategoryId, categoryId);
        }

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

        Note note = noteMapper.selectOne(
                Wrappers.lambdaQuery(Note.class)
                        .eq(Note::getId, noteId)
                        .eq(Note::getUserId, userId)
                        .eq(Note::getDeleted, NOT_DELETED)
        );

        if (note == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        validateCategoryBelongsToCurrentUser(userId, request.categoryId());
        List<Long> tagIds = normalizeAndValidateTagIds(userId, request.tagIds());

        LambdaUpdateWrapper<Note> updateWrapper = Wrappers.lambdaUpdate(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, NOT_DELETED)
                .set(Note::getTitle, request.title().trim())
                .set(Note::getContentMd, request.contentMd())
                .set(Note::getSummary, request.summary())
                .set(Note::getCategoryId, request.categoryId());

        // updatedAt 由 MP 自动更新
        int affectedRows = noteMapper.update(new Note(), updateWrapper);
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        // note-tag 更新
        // 先删除 后插入
        LambdaQueryWrapper<NoteTag> queryByNoteTag = Wrappers.lambdaQuery(NoteTag.class)
                .eq(NoteTag::getNoteId, noteId);

        noteTagMapper.delete(queryByNoteTag);
        bindTags(noteId, tagIds);

        // 暂时再次查询
        Note updateNote = noteMapper.selectOne(
                Wrappers.lambdaQuery(Note.class)
                        .eq(Note::getId, noteId)
                        .eq(Note::getUserId, userId)
                        .eq(Note::getDeleted, NOT_DELETED)
        );

        return toDetailResponse(updateNote);
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

    @Transactional
    public NoteDetailResponse publishNote(Long noteId) {
        Long userId = requireCurrentEnabledUserId();
        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, NOT_DELETED);

        Note note = noteMapper.selectOne(query);
        if (note == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        if (NoteVisibility.PUBLIC.name().equals(note.getVisibility())) {
            return toDetailResponse(note);
        }

        note.setVisibility(NoteVisibility.PUBLIC.name());
        note.setPublishedAt(LocalDateTime.now());

        Note updateNote = new Note();
        int affectedRows = noteMapper.update(updateNote, new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, NOT_DELETED)
                .set(Note::getVisibility, note.getVisibility())
                .set(Note::getPublishedAt, note.getPublishedAt()));
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        note.setUpdatedAt(updateNote.getUpdatedAt());
        return toDetailResponse(note);
    }

    @Transactional
    public NoteDetailResponse unpublishNote(Long noteId) {
        Long userId = requireCurrentEnabledUserId();
        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, NOT_DELETED);

        Note note = noteMapper.selectOne(query);
        if (note == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        if (NoteVisibility.PRIVATE.name().equals(note.getVisibility())) {
            return toDetailResponse(note);
        }

        note.setVisibility(NoteVisibility.PRIVATE.name());

        Note updateNote = new Note();
        int affectedRows = noteMapper.update(updateNote, new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, NOT_DELETED)
                .set(Note::getVisibility, note.getVisibility()));
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        note.setUpdatedAt(updateNote.getUpdatedAt());
        return toDetailResponse(note);
    }

    @Transactional(readOnly = true)
    public PublicNoteListResponse listPublicNotes(long page, long size) {
        Page<Note> pageParam = Page.of(page, size);
        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getVisibility, NoteVisibility.PUBLIC.name())
                .eq(Note::getModerationStatus, NoteModerationStatus.NORMAL.name())
                .eq(Note::getDeleted, NOT_DELETED)
                .isNotNull(Note::getPublishedAt)
                .orderByDesc(Note::getPublishedAt)
                .orderByDesc(Note::getId);

        Page<Note> notePage = noteMapper.selectPage(pageParam, query);

        List<PublicNoteListItemResponse> items = notePage.getRecords()
                .stream()
                .map(this::toPublicListItemResponse)
                .toList();

        return new PublicNoteListResponse(
                items,
                notePage.getTotal(),
                notePage.getCurrent(),
                notePage.getSize()
        );
    }

    @Transactional(readOnly = true)
    public PublicNoteDetailResponse getPublicNoteDetail(Long noteId) {
        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getVisibility, NoteVisibility.PUBLIC.name())
                .isNotNull(Note::getPublishedAt)
                .eq(Note::getModerationStatus, NoteModerationStatus.NORMAL.name())
                .eq(Note::getDeleted, NOT_DELETED);
        Note note = noteMapper.selectOne(query);
        if (note == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        return toPublicDetailResponse(note);
    }

    private NoteListItemResponse toListItemResponse(Note note) {

        return new NoteListItemResponse(
                note.getId(),
                note.getTitle(),
                note.getSummary(),
                note.getCategoryId(),
                note.getVisibility(),
                note.getModerationStatus(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.getPublishedAt()
        );
    }

    private NoteDetailResponse toDetailResponse(Note note) {
        List<NoteTagResponse> tags =
                noteTagMapper.selectTagResponseByNoteId(note.getId());

        return new NoteDetailResponse(
                note.getId(),
                note.getTitle(),
                note.getContentMd(),
                note.getSummary(),
                note.getCategoryId(),
                tags,
                note.getVisibility(),
                note.getModerationStatus(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.getPublishedAt()
        );
    }

    private PublicNoteListItemResponse toPublicListItemResponse(Note note) {
        return new PublicNoteListItemResponse(
                note.getId(),
                note.getTitle(),
                note.getSummary(),
                note.getPublishedAt(),
                note.getUpdatedAt()
        );
    }

    private PublicNoteDetailResponse toPublicDetailResponse(Note note) {
        return new PublicNoteDetailResponse(
                note.getId(),
                note.getTitle(),
                note.getContentMd(),
                note.getSummary(),
                note.getPublishedAt(),
                note.getUpdatedAt()
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

    private List<Long> normalizeAndValidateTagIds(Long userId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        List<Long> distinctTagIds = tagIds.stream().distinct().toList();

        long validTagCount = tagMapper.selectCount(
                Wrappers.lambdaQuery(Tag.class)
                        .eq(Tag::getUserId, userId)
                        .eq(Tag::getDeleted, NOT_DELETED)
                        .in(Tag::getId, distinctTagIds)
        );

        if (validTagCount != distinctTagIds.size()) {
            throw new BizException(ErrorCode.TAG_NOT_FOUND);
        }

        return distinctTagIds;
    }

    private void bindTags(Long noteId, List<Long> tagIds) {
        for (Long tagId : tagIds) {
            NoteTag noteTag = new NoteTag();
            noteTag.setNoteId(noteId);
            noteTag.setTagId(tagId);
            noteTagMapper.insert(noteTag);
        }
    }

    private void validateCategoryBelongsToCurrentUser(Long userId, Long categoryId) {
        if (categoryId == null) {
            return;
        }

        LambdaQueryWrapper<Category> query = Wrappers.lambdaQuery(Category.class)
                .eq(Category::getId, categoryId)
                .eq(Category::getUserId, userId)
                .eq(Category::getDeleted, NOT_DELETED);

        if (categoryMapper.selectCount(query) == 0) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }
}
