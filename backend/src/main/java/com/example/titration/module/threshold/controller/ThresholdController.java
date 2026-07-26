package com.example.titration.module.threshold.controller;

import com.example.titration.common.result.R;
import com.example.titration.module.threshold.entity.ThresholdTemplate;
import com.example.titration.module.threshold.service.ThresholdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/threshold-templates")
@RequiredArgsConstructor
@Tag(name = "阈值模板", description = "HSV 阈值模板管理接口")
public class ThresholdController {

    private final ThresholdService thresholdService;

    @GetMapping
    @Operation(summary = "获取阈值模板列表")
    public R<Object> list() {
        return R.ok(thresholdService.listTemplates());
    }

    @GetMapping("/default")
    @Operation(summary = "获取默认阈值模板")
    public R<ThresholdTemplate> getDefault() {
        return R.ok(thresholdService.getDefaultTemplate());
    }

    @PostMapping
    @Operation(summary = "创建阈值模板")
    @PreAuthorize("hasRole('ADMIN')")
    public R<ThresholdTemplate> create(@RequestBody ThresholdTemplate template) {
        return R.ok("创建成功", thresholdService.createTemplate(template));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新阈值模板")
    @PreAuthorize("hasRole('ADMIN')")
    public R<ThresholdTemplate> update(@PathVariable Long id, @RequestBody ThresholdTemplate template) {
        return R.ok("更新成功", thresholdService.updateTemplate(id, template));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除阈值模板")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        thresholdService.deleteTemplate(id);
        return R.ok("删除成功");
    }
}
