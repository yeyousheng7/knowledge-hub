package com.yousheng.knowledgehub.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yousheng.knowledgehub.auth.dto.LoginResponse;
import com.yousheng.knowledgehub.auth.dto.LoginUserResponse;
import com.yousheng.knowledgehub.auth.dto.RegisterRequest;
import com.yousheng.knowledgehub.auth.dto.RegisterResponse;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.config.InviteCodeProperties;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.security.JwtConstants;
import com.yousheng.knowledgehub.security.JwtToken;
import com.yousheng.knowledgehub.security.JwtTokenProvider;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserRole;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class AuthService {
    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final InviteCodeProperties inviteCodeProperties;

    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        String username = registerRequest.username();
        String password = registerRequest.password();
        String nickname = registerRequest.nickname() == null
                ? registerRequest.username()
                : registerRequest.nickname();

        if(!inviteCodeProperties.getInviteCode().equals(registerRequest.inviteCode())) {
            throw new BizException(ErrorCode.INVALID_INVITE_CODE);
        }

        LambdaQueryWrapper<AppUser> wrapper = Wrappers.lambdaQuery(AppUser.class);
        wrapper.eq(AppUser::getUsername, username);

        Long existCnt = appUserMapper.selectCount(wrapper);
        if (existCnt > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        String passwordHash = passwordEncoder.encode(password);
        LocalDateTime now = LocalDateTime.now();

        AppUser appUser = new AppUser();
        appUser.setUsername(username);
        appUser.setPasswordHash(passwordHash);
        appUser.setNickname(nickname);
        appUser.setRole(UserRole.USER.name());
        appUser.setStatus(UserStatus.ENABLED.name());
        appUser.setCreatedAt(now);
        appUser.setUpdatedAt(now);

        try {
            appUserMapper.insert(appUser);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        RegisterResponse registerResponse = new RegisterResponse(
                appUser.getId(),
                appUser.getUsername(),
                appUser.getNickname()
        );

        return registerResponse;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String username, String password) {
        LambdaQueryWrapper<AppUser> queryByUsername = Wrappers.lambdaQuery(AppUser.class);
        queryByUsername.eq(AppUser::getUsername, username);
        AppUser user = appUserMapper.selectOne(queryByUsername);
        if (user == null) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }

        JwtToken jwtToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );

        LoginResponse loginResponse = new LoginResponse(
                jwtToken.accessToken(),
                JwtConstants.TOKEN_TYPE_BEARER,
                jwtToken.expiresIn(),
                new LoginUserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getNickname(),
                        user.getRole()
                )
        );

        return loginResponse;
    }

    @Transactional(readOnly = true)
    public LoginUserResponse getCurrentUser() {
        Long userId = CurrentUser.getUserId();
        AppUser user = appUserMapper.selectById(userId);
        if(user == null) {
            // 这里不返回 USER_NOT_FOUND
            // 返回 UNAUTHORIZED 代表当前 Token 对应的身份已失效
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        if(!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }

        return new LoginUserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRole()
        );

    }
}
