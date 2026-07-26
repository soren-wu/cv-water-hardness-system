package com.example.titration.module.experiment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.titration.common.exception.BusinessException;
import com.example.titration.module.experiment.entity.Experiment;
import com.example.titration.module.experiment.entity.ExperimentFile;
import com.example.titration.module.experiment.mapper.ExperimentMapper;
import com.example.titration.module.experiment.mapper.ExperimentFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final ExperimentMapper experimentMapper;
    private final ExperimentFileMapper experimentFileMapper;

    public Page<Experiment> listExperiments(int page, int size, Long taskId,
                                             String recognitionStatus, String submitStatus,
                                             String role, Long userId) {
        LambdaQueryWrapper<Experiment> wrapper = new LambdaQueryWrapper<>();
        if (role != null && role.contains("STUDENT")) {
            wrapper.eq(Experiment::getStudentId, userId);
        }
        if (taskId != null) {
            wrapper.eq(Experiment::getTaskId, taskId);
        }
        if (recognitionStatus != null && !recognitionStatus.isEmpty()) {
            wrapper.eq(Experiment::getRecognitionStatus, recognitionStatus);
        }
        if (submitStatus != null && !submitStatus.isEmpty()) {
            wrapper.eq(Experiment::getSubmitStatus, submitStatus);
        }
        wrapper.orderByDesc(Experiment::getCreatedAt);
        return experimentMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Experiment getExperimentById(Long id) {
        Experiment experiment = experimentMapper.selectById(id);
        if (experiment == null) {
            throw new BusinessException(404, "实验记录不存在");
        }
        return experiment;
    }

    public Experiment submitExperiment(Experiment experiment, Long userId) {
        experiment.setStudentId(userId);
        experiment.setSubmittedAt(LocalDateTime.now());
        experiment.setSubmitStatus("SUBMITTED");
        experimentMapper.insert(experiment);
        return experiment;
    }

    public Experiment updateExperiment(Long id, Experiment experiment, Long userId, String role) {
        Experiment existing = getExperimentById(id);
        if (role != null && role.contains("STUDENT") && !existing.getStudentId().equals(userId)) {
            throw new BusinessException(403, "无权修改他人实验记录");
        }
        experiment.setId(id);
        experimentMapper.updateById(experiment);
        return experimentMapper.selectById(id);
    }

    public void deleteExperiment(Long id) {
        if (experimentMapper.deleteById(id) == 0) {
            throw new BusinessException(404, "实验记录不存在");
        }
    }

    public List<ExperimentFile> getExperimentFiles(Long experimentId) {
        LambdaQueryWrapper<ExperimentFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExperimentFile::getExperimentId, experimentId);
        return experimentFileMapper.selectList(wrapper);
    }
}
