package com.yousheng.knowledgehub.tag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.tag.dto.TagCreateRequest;
import com.yousheng.knowledgehub.tag.dto.TagCreateResponse;
import com.yousheng.knowledgehub.tag.dto.TagListItemResponse;
import com.yousheng.knowledgehub.tag.dto.TagListResponse;
import com.yousheng.knowledgehub.tag.entity.Tag;
import com.yousheng.knowledgehub.tag.mapper.TagMapper;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class TagService {
    private final TagMapper tagMapper;
    private final AppUserMapper appUserMapper;
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
