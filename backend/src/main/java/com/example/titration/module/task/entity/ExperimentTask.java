package com.example.titration.module.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("experiment_tasks")
public class ExperimentTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String requirement;
    private Long teacherId;
    private Long targetClassId;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime deadlineAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
