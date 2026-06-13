package com.yousheng.knowledgehub.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.user.dto.UserProfileResponse;
import com.yousheng.knowledgehub.user.dto.UserProfileUpdateRequest;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserProfileService {
    private final AppUserMapper appUserMapper;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        AppUser user = appUserMapper.selectById(CurrentUser.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }

        return toUserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UserProfileUpdateRequest request) {
        requireCurrentUserEnabled();

        LambdaUpdateWrapper<AppUser> updateWrapper = Wrappers.lambdaUpdate(AppUser.class)
                .eq(AppUser::getId, CurrentUser.getUserId())
                .eq(AppUser::getStatus, UserStatus.ENABLED.name());

        if (request.nickname() != null) {
            updateWrapper.set(AppUser::getNickname, request.nickname());
        }

        if (request.bio() != null) {
            updateWrapper.set(AppUser::getBio, request.bio());
        }

        int affectedRows = appUserMapper.update(new AppUser(), updateWrapper);
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        LambdaQueryWrapper<AppUser> queryWrapper = Wrappers.lambdaQuery(AppUser.class)
                .eq(AppUser::getId, CurrentUser.getUserId())
                .eq(AppUser::getStatus, UserStatus.ENABLED.name());

        AppUser appUser = appUserMapper.selectOne(queryWrapper);
        return toUserProfileResponse(appUser);
    }

    private void requireCurrentUserEnabled() {
        AppUser user = appUserMapper.selectById(CurrentUser.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
    }

    private UserProfileResponse toUserProfileResponse(AppUser user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getBio(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
