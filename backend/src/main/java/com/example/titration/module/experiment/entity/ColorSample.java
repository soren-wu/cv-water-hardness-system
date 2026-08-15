package com.example.titration.module.experiment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("color_samples")
public class ColorSample {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long experimentId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime sampleTime;
    private Integer frameIndex;
    private BigDecimal hue;
    private BigDecimal saturation;
    private BigDecimal brightness;
    private BigDecimal confidence;
    private String stateLabel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
