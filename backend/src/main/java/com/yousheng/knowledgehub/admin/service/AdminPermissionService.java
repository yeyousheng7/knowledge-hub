package com.yousheng.knowledgehub.admin.service;

import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserRole;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {
    private final AppUserMapper appUserMapper;

    public void requireCurrentAdminEnabled() {
        Long currentUserId = CurrentUser.getUserId();

        AppUser currentUser = appUserMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        if (!UserStatus.ENABLED.name().equals(currentUser.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }

        if (!UserRole.ADMIN.name().equals(currentUser.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}
