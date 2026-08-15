package com.example.titration.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.titration.common.exception.BusinessException;
import com.example.titration.module.auth.dto.LoginRequest;
import com.example.titration.module.auth.dto.LoginResponse;
import com.example.titration.module.auth.dto.UserVO;
import com.example.titration.module.log.service.OperationLogService;
import com.example.titration.module.user.entity.User;
import com.example.titration.module.user.mapper.UserMapper;
import com.example.titration.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OperationLogService operationLogService;

    @Value("${jwt.expiration}")
    private long expiration;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );

        if (user == null) {
            throw new BusinessException(401, "账号或密码错误");
        }

        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException(401, "账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "账号或密码错误");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 记录登录日志
        operationLogService.record(user.getId(), "登录",
                "用户 " + user.getUsername() + " 登录系统", null);

        return LoginResponse.of(
                token,
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                expiration
        );
    }

    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException(401, "账号已被禁用");
        }
        return UserVO.fromEntity(user);
    }
}
