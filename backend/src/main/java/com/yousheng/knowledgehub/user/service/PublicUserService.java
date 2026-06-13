package com.yousheng.knowledgehub.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.user.dto.PublicUserProfileResponse;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PublicUserService {
    private final AppUserMapper appUserMapper;

    @Transactional(readOnly = true)
    public PublicUserProfileResponse getPublicUserProfile(String username) {
        AppUser user = requireEnabledUserByUsername(username);
        return new PublicUserProfileResponse(
                user.getUsername(),
                user.getNickname(),
                user.getBio(),
                user.getCreatedAt()
        );
    }

    private AppUser requireEnabledUserByUsername(String username) {
        String normalizedUsername = username.trim();
        LambdaQueryWrapper<AppUser> query = Wrappers.lambdaQuery(AppUser.class)
                .eq(AppUser::getStatus, UserStatus.ENABLED.name())
                .eq(AppUser::getUsername, normalizedUsername);
        AppUser user = appUserMapper.selectOne(query);

        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

}
