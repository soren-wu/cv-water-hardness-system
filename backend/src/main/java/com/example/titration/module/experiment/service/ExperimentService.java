package com.example.titration.module.experiment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.titration.common.exception.BusinessException;
import com.example.titration.module.experiment.entity.ColorSample;
import com.example.titration.module.experiment.entity.Experiment;
import com.example.titration.module.experiment.entity.ExperimentFile;
import com.example.titration.module.experiment.entity.StateEvent;
import com.example.titration.module.experiment.mapper.ColorSampleMapper;
import com.example.titration.module.experiment.mapper.ExperimentMapper;
import com.example.titration.module.experiment.mapper.ExperimentFileMapper;
import com.example.titration.module.experiment.mapper.StateEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final ExperimentMapper experimentMapper;
    private final ExperimentFileMapper experimentFileMapper;
    private final ColorSampleMapper colorSampleMapper;
    private final StateEventMapper stateEventMapper;

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

    // ---------- HSV 采样数据 ----------

    public int saveSamples(Long experimentId, List<ColorSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return 0;
        }
        samples.forEach(s -> s.setExperimentId(experimentId));
        int count = 0;
        for (ColorSample sample : samples) {
            colorSampleMapper.insert(sample);
            count++;
        }
        return count;
    }

    public List<ColorSample> getSamples(Long experimentId) {
        LambdaQueryWrapper<ColorSample> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ColorSample::getExperimentId, experimentId)
                .orderByAsc(ColorSample::getFrameIndex);
        return colorSampleMapper.selectList(wrapper);
    }

    // ---------- 状态事件 ----------

    public int saveEvents(Long experimentId, List<StateEvent> events) {
        if (events == null || events.isEmpty()) {
            return 0;
        }
        events.forEach(e -> e.setExperimentId(experimentId));
        int count = 0;
        for (StateEvent event : events) {
            stateEventMapper.insert(event);
            count++;
        }
        return count;
    }

    public List<StateEvent> getEvents(Long experimentId) {
        LambdaQueryWrapper<StateEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StateEvent::getExperimentId, experimentId)
                .orderByAsc(StateEvent::getOccurredAt);
        return stateEventMapper.selectList(wrapper);
    }
}
