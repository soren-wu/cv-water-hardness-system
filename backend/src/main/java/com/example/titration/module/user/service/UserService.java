package com.example.titration.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.titration.common.exception.BusinessException;
import com.example.titration.module.user.entity.User;
import com.example.titration.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public Page<User> listUsers(int page, int size, String role, String status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByAsc(User::getRole).orderByAsc(User::getId);
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    public User getByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    public User createUser(User user) {
        User existing = getByUsername(user.getUsername());
        if (existing != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setStatus(user.getStatus() != null ? user.getStatus() : "ENABLED");
        userMapper.insert(user);
        return user;
    }

    public User updateUser(Long id, User user) {
        getUserById(id);
        user.setId(id);
        // 不允许通过此接口修改密码
        user.setPasswordHash(null);
        userMapper.updateById(user);
        return userMapper.selectById(id);
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        if ("ADMIN".equals(user.getRole())) {
            List<User> admins = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getRole, "ADMIN")
                            .eq(User::getStatus, "ENABLED"));
            if (admins.size() <= 1) {
                throw new BusinessException(400, "不能删除最后一个管理员");
            }
        }
        userMapper.deleteById(id);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(400, "原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public void disableUser(Long id) {
        User user = getUserById(id);
        user.setStatus("DISABLED");
        userMapper.updateById(user);
    }

    public void enableUser(Long id) {
        User user = getUserById(id);
        user.setStatus("ENABLED");
        userMapper.updateById(user);
    }
}
