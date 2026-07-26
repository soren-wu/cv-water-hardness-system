package com.example.titration.module.threshold.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("threshold_templates")
public class ThresholdTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateName;
    private String version;
    private BigDecimal redHMin;
    private BigDecimal redHMax;
    private BigDecimal purpleHMin;
    private BigDecimal purpleHMax;
    private BigDecimal blueHMin;
    private BigDecimal blueHMax;
    private BigDecimal minSaturation;
    private BigDecimal minBrightness;
    private Integer stableDurationSeconds;
    private Integer isDefault;
    private String status;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
