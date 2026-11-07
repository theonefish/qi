package com.example.qi.module.auth.controller;

import com.example.qi.common.Result;
import com.example.qi.module.auth.dto.LoginRequest;
import com.example.qi.module.auth.dto.LoginResponse;
import com.example.qi.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    /** 登录（SaTokenConfig 中排除拦截） */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<String> me() {
        return Result.ok(authService.currentOperator().name());
    }
}
