package com.example.titration.module.user.controller;

import com.example.titration.common.result.R;
import com.example.titration.module.user.entity.ClassEntity;
import com.example.titration.module.user.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "班级管理", description = "班级管理接口（管理员）")
public class ClassController {

    private final ClassService classService;

    @GetMapping
    @Operation(summary = "获取班级列表")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public R<List<ClassEntity>> list() {
        return R.ok(classService.listClasses());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取班级详情")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public R<ClassEntity> detail(@PathVariable Long id) {
        return R.ok(classService.getClassById(id));
    }

    @PostMapping
    @Operation(summary = "创建班级")
    @PreAuthorize("hasRole('ADMIN')")
    public R<ClassEntity> create(@RequestBody ClassEntity clazz) {
        return R.ok("创建成功", classService.createClass(clazz));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新班级")
    @PreAuthorize("hasRole('ADMIN')")
    public R<ClassEntity> update(@PathVariable Long id, @RequestBody ClassEntity clazz) {
        return R.ok("更新成功", classService.updateClass(id, clazz));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除班级")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        classService.deleteClass(id);
        return R.ok("删除成功");
    }
}
