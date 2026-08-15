package com.example.titration.module.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.titration.module.log.entity.OperationLog;
import com.example.titration.module.log.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    /** 记录一条操作日志。 */
    public void record(Long userId, String operationType, String content, String requestIp) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType(operationType);
        log.setOperationContent(content);
        log.setRequestIp(requestIp);
        operationLogMapper.insert(log);
    }

    /** 分页查询操作日志。 */
    public Page<OperationLog> listLogs(int page, int size, String operationType) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        return operationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
