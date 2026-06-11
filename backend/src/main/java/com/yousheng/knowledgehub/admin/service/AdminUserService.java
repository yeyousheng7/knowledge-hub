package com.yousheng.knowledgehub.admin.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yousheng.knowledgehub.admin.dto.AdminUserStatusResponse;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserRole;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AdminUserService {
    private final AdminPermissionService adminPermissionService;

    private final AppUserMapper appUserMapper;

    @Transactional
    public AdminUserStatusResponse disableAppUser(Long userId) {
        adminPermissionService.requireCurrentAdminEnabled();

        if (CurrentUser.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        AppUser user = appUserMapper.selectOne(
                Wrappers.lambdaQuery(AppUser.class)
                        .eq(AppUser::getId, userId)
                        .eq(AppUser::getRole, UserRole.USER.name())
        );

        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        if (UserStatus.DISABLED.name().equals(user.getStatus())) {
            return new AdminUserStatusResponse(
                    userId,
                    user.getUsername(),
                    user.getStatus()
            );
        }

        LambdaUpdateWrapper<AppUser> updateWrapper = Wrappers.lambdaUpdate(AppUser.class)
                .eq(AppUser::getId, userId)
                .eq(AppUser::getStatus, UserStatus.ENABLED.name())
                .eq(AppUser::getRole, UserRole.USER.name())
                .set(AppUser::getStatus, UserStatus.DISABLED.name());

        int affectedRows = appUserMapper.update(new AppUser(), updateWrapper);
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        return new AdminUserStatusResponse(
                userId,
                user.getUsername(),
                UserStatus.DISABLED.name()
        );
    }

    @Transactional
    public AdminUserStatusResponse enableAppUser(Long userId) {
        adminPermissionService.requireCurrentAdminEnabled();

        if (CurrentUser.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        AppUser user = appUserMapper.selectOne(
                Wrappers.lambdaQuery(AppUser.class)
                        .eq(AppUser::getId, userId)
                        .eq(AppUser::getRole, UserRole.USER.name())
        );
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        if (UserStatus.ENABLED.name().equals(user.getStatus())) {
            return new AdminUserStatusResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getStatus()
            );
        }

        LambdaUpdateWrapper<AppUser> updateWrapper = Wrappers.lambdaUpdate(AppUser.class)
                .eq(AppUser::getId, userId)
                .eq(AppUser::getRole, UserRole.USER.name())
                .eq(AppUser::getStatus, UserStatus.DISABLED.name())
                .set(AppUser::getStatus, UserStatus.ENABLED.name());

        int affectedRows = appUserMapper.update(new AppUser(), updateWrapper);
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        return new AdminUserStatusResponse(
                userId,
                user.getUsername(),
                UserStatus.ENABLED.name()
        );
    }

}
