package com.example.titration.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.titration.common.exception.BusinessException;
import com.example.titration.module.task.entity.ExperimentTask;
import com.example.titration.module.task.entity.TaskAssignment;
import com.example.titration.module.task.mapper.ExperimentTaskMapper;
import com.example.titration.module.task.mapper.TaskAssignmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final ExperimentTaskMapper taskMapper;
    private final TaskAssignmentMapper assignmentMapper;

    public Page<ExperimentTask> listTasks(int page, int size, String status, String role, Long userId) {
        LambdaQueryWrapper<ExperimentTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ExperimentTask::getStatus, status);
        }
        if (role != null && role.contains("STUDENT")) {
            wrapper.eq(ExperimentTask::getStatus, "PUBLISHED");
        }
        wrapper.orderByDesc(ExperimentTask::getCreatedAt);
        return taskMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public ExperimentTask getTaskById(Long id) {
        ExperimentTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        return task;
    }

    public ExperimentTask createTask(ExperimentTask task, Long teacherId) {
        task.setTeacherId(teacherId);
        task.setStatus("DRAFT");
        taskMapper.insert(task);
        return task;
    }

    public ExperimentTask updateTask(Long id, ExperimentTask task) {
        if (taskMapper.selectById(id) == null) {
            throw new BusinessException(404, "任务不存在");
        }
        task.setId(id);
        taskMapper.updateById(task);
        return taskMapper.selectById(id);
    }

    public void deleteTask(Long id) {
        if (taskMapper.deleteById(id) == 0) {
            throw new BusinessException(404, "任务不存在");
        }
    }

    // ===== 任务分配 =====

    public void assignTask(Long taskId, Long studentId) {
        TaskAssignment assignment = new TaskAssignment();
        assignment.setTaskId(taskId);
        assignment.setStudentId(studentId);
        assignment.setStatus("TODO");
        assignmentMapper.insert(assignment);
    }

    public void batchAssign(Long taskId, List<Long> studentIds) {
        for (Long studentId : studentIds) {
            assignTask(taskId, studentId);
        }
    }

    public List<TaskAssignment> getAssignmentsByTask(Long taskId) {
        LambdaQueryWrapper<TaskAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskAssignment::getTaskId, taskId);
        return assignmentMapper.selectList(wrapper);
    }

    public List<TaskAssignment> getAssignmentsByStudent(Long studentId) {
        LambdaQueryWrapper<TaskAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskAssignment::getStudentId, studentId);
        return assignmentMapper.selectList(wrapper);
    }
}
