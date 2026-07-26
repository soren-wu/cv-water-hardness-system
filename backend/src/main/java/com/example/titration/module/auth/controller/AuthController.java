package com.example.titration.module.auth.controller;

import com.example.titration.common.result.R;
import com.example.titration.module.auth.dto.LoginRequest;
import com.example.titration.module.auth.dto.LoginResponse;
import com.example.titration.module.auth.dto.UserVO;
import com.example.titration.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "登录认证相关接口")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return R.ok("登录成功", response);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public R<UserVO> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        UserVO userVO = authService.getCurrentUser(userId);
        return R.ok(userVO);
    }
}
