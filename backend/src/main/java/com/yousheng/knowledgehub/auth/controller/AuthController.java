package com.yousheng.knowledgehub.auth.controller;

import com.yousheng.knowledgehub.auth.dto.*;
import com.yousheng.knowledgehub.auth.service.AuthService;
import com.yousheng.knowledgehub.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/me")
    public ApiResponse<LoginUserResponse> me() {
        return ApiResponse.ok(authService.getCurrentUser());
    }
}
