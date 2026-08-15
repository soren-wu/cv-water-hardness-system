package com.example.titration.module.log.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.titration.common.result.R;
import com.example.titration.module.log.entity.OperationLog;
import com.example.titration.module.log.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "操作日志查询接口")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "查询操作日志（管理员）")
    public R<Page<OperationLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String operationType) {
        return R.ok(operationLogService.listLogs(page, size, operationType));
    }
}
