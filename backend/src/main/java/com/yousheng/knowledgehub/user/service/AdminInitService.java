package com.yousheng.knowledgehub.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yousheng.knowledgehub.config.AdminInitProperties;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserRole;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AdminInitService {
    private final AdminInitProperties adminInitProperties;
    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void initializeAdminIfNecessary() {
        if (!adminInitProperties.isEnabled()) {
            return;
        }

        validateAdminCredentials(
                adminInitProperties.getUsername(),
                adminInitProperties.getPassword(),
                adminInitProperties.getNickname()
        );

        String nickname = adminInitProperties.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = adminInitProperties.getUsername();
        }

        Long existedAdminUserCnt = appUserMapper.selectCount(
                Wrappers.lambdaQuery(AppUser.class)
                        .eq(AppUser::getRole, UserRole.ADMIN.name())
                        .eq(AppUser::getStatus, UserStatus.ENABLED.name())
        );

        if (existedAdminUserCnt != null && existedAdminUserCnt > 0) {
            return;
        }


        AppUser user = appUserMapper.selectOne(
                Wrappers.lambdaQuery(AppUser.class)
                        .eq(AppUser::getUsername, adminInitProperties.getUsername())
        );

        if (user != null) {
            String msg = "用户名 [" + adminInitProperties.getUsername() + "] 被占用";
            throw new IllegalStateException(buildErrorMessage(msg));
        }

        AppUser adminUser = new AppUser();
        adminUser.setUsername(adminInitProperties.getUsername());
        adminUser.setPasswordHash(passwordEncoder.encode(adminInitProperties.getPassword()));
        adminUser.setNickname(nickname);
        adminUser.setRole(UserRole.ADMIN.name());
        adminUser.setStatus(UserStatus.ENABLED.name());

        appUserMapper.insert(adminUser);
    }

    private void validateAdminCredentials(String username, String password, String nickname) {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException(buildErrorMessage("用户名不能为空"));
        }

        if (username.length() < 3 || username.length() > 30) {
            throw new IllegalStateException(buildErrorMessage("用户名长度必须在 3 - 30 个字符之间"));
        }

        if (!username.matches("^[0-9a-zA-Z_]+$")) {
            throw new IllegalStateException(buildErrorMessage("用户名只能包含字母、数字和下划线"));
        }

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(buildErrorMessage("密码不能为空"));
        }

        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException(buildErrorMessage("密码长度必须在 8 - 72 个字符之间"));
        }

        if (nickname == null || nickname.isBlank()) {
            return;
        }

        if (nickname.length() < 3 || nickname.length() > 30) {
            throw new IllegalStateException(buildErrorMessage("昵称长度必须在 3 - 30 个字符之间"));
        }
    }

    private String buildErrorMessage(String msg) {
        return "[管理员初始化配置错误]: " + msg;
    }
}
