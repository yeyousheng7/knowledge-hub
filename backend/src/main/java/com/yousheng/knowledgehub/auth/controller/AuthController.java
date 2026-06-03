package com.yousheng.knowledgehub.auth.controller;

import com.yousheng.knowledgehub.auth.dto.*;
import com.yousheng.knowledgehub.auth.service.AuthService;
import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.config.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "认证与当前用户接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "注册账号")
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ApiResponse.ok(authService.register(registerRequest));
    }

    @Operation(summary = "登录账号")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.username();
        String password = loginRequest.password();
        return ApiResponse.ok(authService.login(username, password));
    }

    @Operation(summary = "获取当前登录用户")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/me")
    public ApiResponse<LoginUserResponse> me() {
        return ApiResponse.ok(authService.getCurrentUser());
    }
}
