package com.yousheng.knowledgehub.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yousheng.knowledgehub.admin.dto.AdminUserItemResponse;
import com.yousheng.knowledgehub.admin.dto.AdminUserListResponse;
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

import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Service
public class AdminUserService {
    private final AdminPermissionService adminPermissionService;

    private final AppUserMapper appUserMapper;

    public AdminUserListResponse getAppUserList(long page, long size, String keyword, String status) {
        adminPermissionService.requireCurrentAdminEnabled();

        Page<AppUser> pageParam = Page.of(page, size);
        LambdaQueryWrapper<AppUser> query = Wrappers.lambdaQuery(AppUser.class)
                .orderByAsc(AppUser::getCreatedAt)
                .orderByDesc(AppUser::getId);

        String normalizedStatus = status == null ? "" : status.trim();
        if (!normalizedStatus.isBlank()) {
            query.eq(AppUser::getStatus, normalizedStatus);
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
            String pattern = "%" + normalizedKeyword + "%";

            query.and(wrapper -> wrapper
                    .apply("LOWER(username) LIKE {0} ESCAPE '!'", pattern)
                    .or()
                    .apply("LOWER(nickname) LIKE {0} ESCAPE '!'", pattern)
            );
        }

        Page<AppUser> appUserPage = appUserMapper.selectPage(pageParam, query);

        List<AdminUserItemResponse> items = appUserPage.getRecords()
                .stream()
                .map(user -> new AdminUserItemResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getNickname(),
                        user.getRole(),
                        user.getStatus(),
                        user.getCreatedAt(),
                        user.getUpdatedAt()
                ))
                .toList();

        return new AdminUserListResponse(
                items,
                appUserPage.getTotal(),
                appUserPage.getCurrent(),
                appUserPage.getSize()
        );

    }

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
