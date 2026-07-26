package com.example.titration.module.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_assignments")
public class TaskAssignment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long studentId;
    private String status;
    private LocalDateTime assignedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
