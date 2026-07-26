package com.example.titration.module.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.titration.common.result.R;
import com.example.titration.module.user.entity.User;
import com.example.titration.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户管理接口（管理员）")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "获取用户列表")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Page<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return R.ok(userService.listUsers(page, size, role, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    @PreAuthorize("hasRole('ADMIN')")
    public R<User> detail(@PathVariable Long id) {
        return R.ok(userService.getUserById(id));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    @PreAuthorize("hasRole('ADMIN')")
    public R<User> create(@Valid @RequestBody User user) {
        return R.ok("创建成功", userService.createUser(user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息")
    @PreAuthorize("hasRole('ADMIN')")
    public R<User> update(@PathVariable Long id, @RequestBody User user) {
        return R.ok("更新成功", userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok("删除成功");
    }

    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "启用/禁用用户")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if ("DISABLED".equals(status)) {
            userService.disableUser(id);
        } else {
            userService.enableUser(id);
        }
        return R.ok("操作成功");
    }

    @PutMapping("/change-password")
    @Operation(summary = "修改当前用户密码")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return R.ok("密码修改成功");
    }
}
