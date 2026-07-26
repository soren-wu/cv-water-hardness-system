package com.example.titration.module.task.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.titration.common.result.R;
import com.example.titration.module.task.entity.ExperimentTask;
import com.example.titration.module.task.entity.TaskAssignment;
import com.example.titration.module.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "实验任务", description = "实验任务管理接口")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "获取实验任务列表")
    public R<Page<ExperimentTask>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().toString();
        Long userId = (Long) auth.getPrincipal();
        return R.ok(taskService.listTasks(page, size, status, role, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取实验任务详情")
    public R<ExperimentTask> detail(@PathVariable Long id) {
        return R.ok(taskService.getTaskById(id));
    }

    @PostMapping
    @Operation(summary = "创建实验任务")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public R<ExperimentTask> create(@RequestBody ExperimentTask task) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return R.ok("创建成功", taskService.createTask(task, (Long) auth.getPrincipal()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新实验任务")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public R<ExperimentTask> update(@PathVariable Long id, @RequestBody ExperimentTask task) {
        return R.ok("更新成功", taskService.updateTask(id, task));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除实验任务")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        taskService.deleteTask(id);
        return R.ok("删除成功");
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "分配任务给学生")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public R<Void> assign(@PathVariable Long id, @RequestBody List<Long> studentIds) {
        taskService.batchAssign(id, studentIds);
        return R.ok("分配成功");
    }

    @GetMapping("/{id}/assignments")
    @Operation(summary = "查看任务分配情况")
    public R<List<TaskAssignment>> assignments(@PathVariable Long id) {
        return R.ok(taskService.getAssignmentsByTask(id));
    }
}
