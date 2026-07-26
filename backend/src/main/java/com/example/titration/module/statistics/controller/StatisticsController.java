package com.example.titration.module.statistics.controller;

import com.example.titration.common.result.R;
import com.example.titration.module.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "数据统计", description = "教学数据统计接口")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    @Operation(summary = "获取教学概览数据")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public R<Map<String, Object>> overview() {
        return R.ok(statisticsService.getOverview());
    }
}
