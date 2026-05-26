package com.yousheng.knowledgehub.auth.controller;

import com.yousheng.knowledgehub.auth.dto.LoginRequest;
import com.yousheng.knowledgehub.auth.dto.LoginResponse;
import com.yousheng.knowledgehub.auth.dto.RegisterRequest;
import com.yousheng.knowledgehub.auth.dto.RegisterResponse;
import com.yousheng.knowledgehub.auth.service.AuthService;
import com.yousheng.knowledgehub.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ApiResponse.ok(authService.register(registerRequest));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.username();
        String password = loginRequest.password();
        return ApiResponse.ok(authService.login(username, password));
    }
}
