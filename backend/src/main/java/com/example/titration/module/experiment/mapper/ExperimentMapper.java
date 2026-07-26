package com.example.titration.module.experiment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.titration.module.experiment.entity.Experiment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExperimentMapper extends BaseMapper<Experiment> {
}
