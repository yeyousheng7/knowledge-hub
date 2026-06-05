package com.yousheng.knowledgehub.tag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.tag.dto.*;
import com.yousheng.knowledgehub.tag.entity.NoteTag;
import com.yousheng.knowledgehub.tag.entity.Tag;
import com.yousheng.knowledgehub.tag.mapper.NoteTagMapper;
import com.yousheng.knowledgehub.tag.mapper.TagMapper;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class TagService {
    private final TagMapper tagMapper;
    private final NoteTagMapper noteTagMapper;
    private final AppUserMapper appUserMapper;
    private final int DELETED = 1;
    private final int NOT_DELETED = 0;


    @Transactional
    public TagCreateResponse createTag(TagCreateRequest tagCreateRequest) {
        Long userId = requireCurrentEnabledUserId();
        String tagName = tagCreateRequest.name().trim();

        Tag tag = new Tag();

        tag.setName(tagName);
        tag.setUserId(userId);

        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.TAG_NAME_EXISTS);
        }

        return new TagCreateResponse(
                tag.getId(),
                tag.getName(),
                tag.getCreatedAt(),
                tag.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public TagListResponse listMyTags() {
        Long userId = requireCurrentEnabledUserId();

        LambdaQueryWrapper<Tag> queryWrapper = Wrappers.lambdaQuery(Tag.class)
                .eq(Tag::getUserId, userId)
                .eq(Tag::getDeleted, NOT_DELETED)
                .orderByDesc(Tag::getUpdatedAt)
                .orderByDesc(Tag::getId);

        List<Tag> tags = tagMapper.selectList(queryWrapper);

        List<TagListItemResponse> tagListItemResponses = tags.stream()
                .map(tag -> {
                    return new TagListItemResponse(
                            tag.getId(),
                            tag.getName(),
                            tag.getCreatedAt(),
                            tag.getUpdatedAt()
                    );
                })
                .toList();

        return new TagListResponse(tagListItemResponses);
    }

    @Transactional
    public TagUpdateResponse updateTag(Long tagId, TagUpdateRequest tagUpdateRequest) {
        Long userId = requireCurrentEnabledUserId();
        String tagName = tagUpdateRequest.name().trim();
        LambdaUpdateWrapper<Tag> updateWrapper = Wrappers.lambdaUpdate(Tag.class)
                .eq(Tag::getUserId, userId)
                .eq(Tag::getId, tagId)
                .eq(Tag::getDeleted, NOT_DELETED)
                .set(Tag::getName, tagName);

        int affectedRows = 0;

        try {
            affectedRows = tagMapper.update(new Tag(), updateWrapper);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.TAG_NAME_EXISTS);
        }

        if (affectedRows == 0) {
            throw new BizException(ErrorCode.TAG_NOT_FOUND);
        }

        LambdaQueryWrapper<Tag> queryWrapper = Wrappers.lambdaQuery(Tag.class)
                .eq(Tag::getId, tagId)
                .eq(Tag::getUserId, userId)
                .eq(Tag::getDeleted, NOT_DELETED);
        Tag tag = tagMapper.selectOne(queryWrapper);
        if (tag == null) {
            throw new BizException(ErrorCode.TAG_NOT_FOUND);
        }

        return new TagUpdateResponse(
                tag.getId(),
                tag.getName(),
                tag.getCreatedAt(),
                tag.getUpdatedAt()
        );
    }

    @Transactional
    public void deleteTag(Long tagId) {
        Long userId = requireCurrentEnabledUserId();
        validateTagBelongToUser(tagId, userId);

        // 软删除 tag
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Tag> updateWrapper = Wrappers.lambdaUpdate(Tag.class)
                .eq(Tag::getId, tagId)
                .eq(Tag::getUserId, userId)
                .eq(Tag::getDeleted, NOT_DELETED)
                .set(Tag::getDeleted, DELETED)
                .set(Tag::getDeletedAt, now)
                .set(Tag::getDeletedMarker, tagId);

        int affectedRows = tagMapper.update(new Tag(), updateWrapper);
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.TAG_NOT_FOUND);
        }

        // 物理删除 note-tag 关系
        noteTagMapper.delete(
                Wrappers.lambdaQuery(NoteTag.class)
                        .eq(NoteTag::getTagId, tagId)
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

    private void validateTagBelongToUser(Long tagId, Long userId) {
        LambdaQueryWrapper<Tag> queryWrapper = Wrappers.lambdaQuery(Tag.class)
                .eq(Tag::getId, tagId)
                .eq(Tag::getUserId, userId)
                .eq(Tag::getDeleted, NOT_DELETED);

        Tag tag = tagMapper.selectOne(queryWrapper);
        if (tag == null) {
            throw new BizException(ErrorCode.TAG_NOT_FOUND);
        }
    }
}
