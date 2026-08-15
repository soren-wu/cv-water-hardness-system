package com.example.titration.module.experiment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.titration.common.result.R;
import com.example.titration.module.experiment.entity.ColorSample;
import com.example.titration.module.experiment.entity.Experiment;
import com.example.titration.module.experiment.entity.StateEvent;
import com.example.titration.module.experiment.service.ExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/experiments")
@RequiredArgsConstructor
@Tag(name = "实验记录", description = "实验记录管理接口")
public class ExperimentController {

    private final ExperimentService experimentService;

    @GetMapping
    @Operation(summary = "获取实验记录列表")
    public R<Page<Experiment>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String recognitionStatus,
            @RequestParam(required = false) String submitStatus) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().toString();
        return R.ok(experimentService.listExperiments(page, size, taskId,
                recognitionStatus, submitStatus, role, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取实验记录详情")
    public R<Experiment> detail(@PathVariable Long id) {
        return R.ok(experimentService.getExperimentById(id));
    }

    @PostMapping
    @Operation(summary = "提交实验记录")
    public R<Experiment> submit(@RequestBody Experiment experiment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return R.ok("提交成功", experimentService.submitExperiment(experiment, (Long) auth.getPrincipal()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新实验记录")
    public R<Experiment> update(@PathVariable Long id, @RequestBody Experiment experiment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().toString();
        return R.ok("更新成功", experimentService.updateExperiment(id, experiment, userId, role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除实验记录")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        experimentService.deleteExperiment(id);
        return R.ok("删除成功");
    }

    @GetMapping("/{id}/files")
    @Operation(summary = "获取实验关联文件")
    public R<Object> files(@PathVariable Long id) {
        return R.ok(experimentService.getExperimentFiles(id));
    }

    @PostMapping("/{id}/samples")
    @Operation(summary = "批量保存 HSV 采样数据")
    public R<Integer> saveSamples(@PathVariable Long id, @RequestBody List<ColorSample> samples) {
        return R.ok("保存成功", experimentService.saveSamples(id, samples));
    }

    @GetMapping("/{id}/samples")
    @Operation(summary = "获取实验 HSV 采样数据")
    public R<List<ColorSample>> samples(@PathVariable Long id) {
        return R.ok(experimentService.getSamples(id));
    }

    @PostMapping("/{id}/events")
    @Operation(summary = "批量保存状态事件")
    public R<Integer> saveEvents(@PathVariable Long id, @RequestBody List<StateEvent> events) {
        return R.ok("保存成功", experimentService.saveEvents(id, events));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "获取实验状态事件")
    public R<List<StateEvent>> events(@PathVariable Long id) {
        return R.ok(experimentService.getEvents(id));
    }
}
