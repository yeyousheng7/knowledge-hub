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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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
    public NoteListResponse listMyNotes(long page, long size, Long categoryId, Long tagId, String keyword) {
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

        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
            String pattern = "%" + normalizedKeyword + "%";

            query.and(wrapper -> wrapper
                    .apply("title LIKE {0} ESCAPE '!'", pattern)
                    .or()
                    .apply("summary LIKE {0} ESCAPE '!'", pattern)
                    .or()
                    .apply("content_md LIKE {0} ESCAPE '!'", pattern)
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

        Map<Long, List<PublicNoteTagResponse>> tagsByNoteId;
        Map<Long, PublicNoteAuthorResponse> authorByUserId;

        List<Note> notes = notePage.getRecords();
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
                    // 在 author 信息缺失时返回 null, 与 toPublicAuthorResponse 保持一致
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
                .eq(Category::getDeleted, NOT_DELETED);

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
                .eq(Tag::getDeleted, NOT_DELETED);
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
                        .eq(Tag::getDeleted, NOT_DELETED)
                        .in(Tag::getId, distinctTagIds)
        );

        if (validTagCount != distinctTagIds.size()) {
            throw new BizException(ErrorCode.TAG_NOT_FOUND);
        }

        return distinctTagIds;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }

        String normalizedKeyword = keyword.trim();

        // 替换 Like 特殊符号
        if (!normalizedKeyword.isEmpty()) {
            normalizedKeyword = normalizedKeyword
                    .replace("!", "!!")
                    .replace("%", "!%")
                    .replace("_", "!_");
        }
        return normalizedKeyword;
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
