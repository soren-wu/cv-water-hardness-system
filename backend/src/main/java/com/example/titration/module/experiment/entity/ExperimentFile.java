package com.example.titration.module.experiment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("experiment_files")
public class ExperimentFile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long experimentId;
    private String fileType;
    private String originalName;
    private String storagePath;
    private String contentType;
    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
