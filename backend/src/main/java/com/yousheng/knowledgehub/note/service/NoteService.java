package com.yousheng.knowledgehub.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yousheng.knowledgehub.category.entity.Category;
import com.yousheng.knowledgehub.category.mapper.CategoryMapper;
import com.yousheng.knowledgehub.common.constant.SoftDeleteConstants;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.common.util.SqlLikeUtils;
import com.yousheng.knowledgehub.note.dto.*;
import com.yousheng.knowledgehub.note.entity.Note;
import com.yousheng.knowledgehub.note.enums.NoteModerationStatus;
import com.yousheng.knowledgehub.note.enums.NoteVisibility;
import com.yousheng.knowledgehub.note.mapper.NoteMapper;
import com.yousheng.knowledgehub.note.util.NoteSummaryUtils;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.tag.dto.NoteTagQueryRow;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class NoteService {
    private final AppUserMapper appUserMapper;
    private final NoteMapper noteMapper;
    private final CategoryMapper categoryMapper;
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
        note.setSummary(NoteSummaryUtils.resolveSummary(request.summary(), request.contentMd()));
        note.setCategoryId(request.categoryId());
        note.setVisibility(NoteVisibility.PRIVATE.name());
        note.setModerationStatus(NoteModerationStatus.NORMAL.name());
        note.setDeleted(SoftDeleteConstants.NOT_DELETED);

        noteMapper.insert(note);

        // 设置 note-tag 关系
        if (!tagIds.isEmpty()) {
            bindTags(note.getId(), tagIds);
        }

        List<NoteTagResponse> tags = noteTagMapper.selectTagResponseByNoteId(note.getId());

        return new NoteCreateResponse(
                note.getId(),
                note.getTitle(),
                note.getSummary(),
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
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED);

        Note note = noteMapper.selectOne(query);

        if (note == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        return toDetailResponse(note);

    }

    @Transactional(readOnly = true)
    public NoteListResponse listMyNotes(long page, long size, Long categoryId, Long tagId, String keyword) {
        Long userId = requireCurrentEnabledUserId();
        Page<Note> pageParam = Page.of(page, size);
        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED)
                .orderByDesc(Note::getUpdatedAt)
                .orderByDesc(Note::getId);

        if (categoryId != null) {
            validateCategoryBelongsToCurrentUser(userId, categoryId);
            query.eq(Note::getCategoryId, categoryId);
        }

        if (tagId != null) {
            validateTagBelongsToCurrentUser(userId, tagId);
            List<Long> noteTagIds = noteTagMapper.selectList(Wrappers.lambdaQuery(NoteTag.class).eq(NoteTag::getTagId, tagId))
                    .stream()
                    .map(NoteTag::getNoteId)
                    .toList();

            // 没有符合条件的 notes
            if (noteTagIds.isEmpty()) {
                return new NoteListResponse(List.of(), 0, page, size);
            }

            query.in(Note::getId, noteTagIds);
        }

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

        List<Note> notes = notePage.getRecords();
        Map<Long, List<NoteTagResponse>> tagsByNoteId;
        if (notes.isEmpty()) {
            tagsByNoteId = Map.of();
        } else {
            List<Long> noteIds = notes.stream()
                    .map(Note::getId)
                    .toList();

            tagsByNoteId = noteTagMapper.selectTagRowsByNoteIds(noteIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            NoteTagQueryRow::noteId,
                            Collectors.mapping(
                                    row -> new NoteTagResponse(
                                            row.tagId(),
                                            row.tagName()
                                    ),
                                    Collectors.toList()
                            )
                    ));
        }

        List<NoteListItemResponse> items = notes.stream()
                .map(note -> toListItemResponse(
                        note,
                        tagsByNoteId.getOrDefault(note.getId(), List.of())
                ))
                .toList();


        return new NoteListResponse(
                items,
                notePage.getTotal(),
                notePage.getCurrent(),
                notePage.getSize()
        );
    }

    @Transactional(readOnly = true)
    public NoteListResponse listMyPublishedNotes(long page, long size) {
        Long userId = requireCurrentEnabledUserId();
        Page<Note> pageParam = Page.of(page, size);
        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getUserId, userId)
                .eq(Note::getVisibility, NoteVisibility.PUBLIC.name())
                .eq(Note::getModerationStatus, NoteModerationStatus.NORMAL.name())
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED)
                .isNotNull(Note::getPublishedAt)
                .orderByDesc(Note::getPublishedAt)
                .orderByDesc(Note::getId);

        Page<Note> notePage = noteMapper.selectPage(pageParam, query);
        List<Note> notes = notePage.getRecords();

        Map<Long, List<NoteTagResponse>> tagsByNoteId;
        if (notes.isEmpty()) {
            tagsByNoteId = Map.of();
        } else {
            List<Long> noteIds = notes.stream()
                    .map(Note::getId)
                    .toList();

            tagsByNoteId = noteTagMapper.selectTagRowsByNoteIds(noteIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            NoteTagQueryRow::noteId,
                            Collectors.mapping(
                                    row -> new NoteTagResponse(
                                            row.tagId(),
                                            row.tagName()
                                    ),
                                    Collectors.toList()
                            )
                    ));
        }

        List<NoteListItemResponse> items = notes.stream()
                .map(note -> toListItemResponse(
                        note,
                        tagsByNoteId.getOrDefault(note.getId(), List.of())
                ))
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
                        .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED)
        );

        if (note == null) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        validateCategoryBelongsToCurrentUser(userId, request.categoryId());
        List<Long> tagIds = normalizeAndValidateTagIds(userId, request.tagIds());

        LambdaUpdateWrapper<Note> updateWrapper = Wrappers.lambdaUpdate(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED)
                .set(Note::getTitle, request.title().trim())
                .set(Note::getContentMd, request.contentMd())
                .set(Note::getSummary, NoteSummaryUtils.resolveSummary(request.summary(), request.contentMd()))
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
                        .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED)
        );

        return toDetailResponse(updateNote);
    }

    @Transactional
    public void deleteNote(Long noteId) {
        Long userId = requireCurrentEnabledUserId();
        LocalDateTime now = LocalDateTime.now();

        Note updateNote = new Note();
        updateNote.setDeleted(SoftDeleteConstants.DELETED);
        updateNote.setDeletedAt(now);

        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED);

        int affectedRows = noteMapper.update(updateNote, query);
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        // 删除 note-tag 关系
        noteTagMapper.delete(
                Wrappers.lambdaQuery(NoteTag.class)
                        .eq(NoteTag::getNoteId, noteId)
        );
    }

    @Transactional
    public NoteDetailResponse publishNote(Long noteId) {
        Long userId = requireCurrentEnabledUserId();
        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED);

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
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED)
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
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED);

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
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED)
                .set(Note::getVisibility, note.getVisibility()));
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND);
        }

        note.setUpdatedAt(updateNote.getUpdatedAt());
        return toDetailResponse(note);
    }

    @Transactional
    public NoteBatchUnpublishResult batchUnpublishMyPublishedNotes(List<Long> noteIds) {
        Long userId = requireCurrentEnabledUserId();
        List<Long> normalizedNoteIds = normalizeBatchNoteIds(noteIds);

        List<Note> notes = noteMapper.selectList(Wrappers.lambdaQuery(Note.class)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED)
                .in(Note::getId, normalizedNoteIds));

        if (notes.size() != normalizedNoteIds.size()) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND, "待下架笔记不存在或不属于当前用户");
        }

        Map<Long, Note> notesById = notes.stream()
                .collect(Collectors.toMap(Note::getId, note -> note));
        for (Long noteId : normalizedNoteIds) {
            Note note = notesById.get(noteId);
            if (!isBatchUnpublishable(note)) {
                throw new BizException(ErrorCode.NOTE_NOT_FOUND, "待下架笔记当前不可操作");
            }
        }

        Note updateNote = new Note();
        int affectedRows = noteMapper.update(updateNote, Wrappers.lambdaUpdate(Note.class)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED)
                .in(Note::getId, normalizedNoteIds)
                .eq(Note::getVisibility, NoteVisibility.PUBLIC.name())
                .eq(Note::getModerationStatus, NoteModerationStatus.NORMAL.name())
                .isNotNull(Note::getPublishedAt)
                .set(Note::getVisibility, NoteVisibility.PRIVATE.name()));
        if (affectedRows != normalizedNoteIds.size()) {
            throw new BizException(ErrorCode.NOTE_NOT_FOUND, "待下架笔记当前不可操作");
        }

        List<NoteListItemResponse> affectedItems = normalizedNoteIds.stream()
                .map(notesById::get)
                .map(note -> toListItemResponse(note, List.of()))
                .toList();
        return new NoteBatchUnpublishResult(affectedRows, affectedItems);
    }

    private List<Long> normalizeBatchNoteIds(List<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "待操作笔记不能为空");
        }

        Set<Long> distinct = new LinkedHashSet<>();
        for (Long noteId : noteIds) {
            if (noteId == null || noteId <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "笔记 ID 必须为正数");
            }
            distinct.add(noteId);
        }
        return List.copyOf(distinct);
    }

    private boolean isBatchUnpublishable(Note note) {
        return note != null
                && NoteVisibility.PUBLIC.name().equals(note.getVisibility())
                && NoteModerationStatus.NORMAL.name().equals(note.getModerationStatus())
                && note.getPublishedAt() != null;
    }

    private NoteListItemResponse toListItemResponse(Note note, List<NoteTagResponse> tags) {

        return new NoteListItemResponse(
                note.getId(),
                note.getTitle(),
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

    private void validateCategoryBelongsToCurrentUser(Long userId, Long categoryId) {
        if (categoryId == null) {
            return;
        }

        LambdaQueryWrapper<Category> query = Wrappers.lambdaQuery(Category.class)
                .eq(Category::getId, categoryId)
                .eq(Category::getUserId, userId)
                .eq(Category::getDeleted, SoftDeleteConstants.NOT_DELETED);

        if (categoryMapper.selectCount(query) == 0) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    private void validateTagBelongsToCurrentUser(Long userId, Long tagId) {
        if (tagId == null) {
            return;
        }

        LambdaQueryWrapper<Tag> query = Wrappers.lambdaQuery(Tag.class)
                .eq(Tag::getId, tagId)
                .eq(Tag::getUserId, userId)
                .eq(Tag::getDeleted, SoftDeleteConstants.NOT_DELETED);
        Long cnt = tagMapper.selectCount(query);
        if (cnt == 0) {
            throw new BizException(ErrorCode.TAG_NOT_FOUND);
        }
    }

    private List<Long> normalizeAndValidateTagIds(Long userId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        List<Long> distinctTagIds = tagIds.stream().distinct().toList();

        long validTagCount = tagMapper.selectCount(
                Wrappers.lambdaQuery(Tag.class)
                        .eq(Tag::getUserId, userId)
                        .eq(Tag::getDeleted, SoftDeleteConstants.NOT_DELETED)
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
}
