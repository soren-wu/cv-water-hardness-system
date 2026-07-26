package com.example.titration.module.experiment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("experiments")
public class Experiment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long studentId;
    private Long thresholdTemplateId;
    private String experimentName;
    private String sampleName;
    private String detectMode;
    private String recognitionStatus;
    private String recognitionLabel;
    private String matchedColor;
    private BigDecimal confidence;
    private BigDecimal hue;
    private BigDecimal saturation;
    private BigDecimal brightness;
    private BigDecimal redRatio;
    private BigDecimal purpleRatio;
    private BigDecimal blueRatio;
    private LocalDateTime candidateEndpointAt;
    private LocalDateTime endpointAt;
    private Integer stableDurationSeconds;
    private String submitStatus;
    private LocalDateTime submittedAt;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
