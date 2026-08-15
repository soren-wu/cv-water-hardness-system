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
import com.example.titration.module.review.entity.Review;
import com.example.titration.module.review.mapper.ReviewMapper;
import com.example.titration.module.user.entity.User;
import com.example.titration.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final ExperimentMapper experimentMapper;
    private final ExperimentFileMapper experimentFileMapper;
    private final ColorSampleMapper colorSampleMapper;
    private final StateEventMapper stateEventMapper;
    private final UserMapper userMapper;
    private final ReviewMapper reviewMapper;

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
        } else if (role == null || !role.contains("STUDENT")) {
            // 教师/管理员默认排除学生草稿（只看到已提交/已批阅）
            wrapper.ne(Experiment::getSubmitStatus, "DRAFT");
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
        // 保存为草稿，需学生主动「提交」后教师端才可见
        experiment.setSubmitStatus("DRAFT");
        experimentMapper.insert(experiment);
        return experiment;
    }

    /** 学生主动提交草稿，转为已提交状态（教师端可见）。 */
    public Experiment submitToTeacher(Long id, Long userId, String role) {
        Experiment experiment = getExperimentById(id);
        if (role != null && role.contains("STUDENT") && !experiment.getStudentId().equals(userId)) {
            throw new BusinessException(403, "无权提交他人实验记录");
        }
        if (!"DRAFT".equals(experiment.getSubmitStatus())) {
            throw new BusinessException(400, "该记录已提交，无需重复提交");
        }
        experiment.setSubmitStatus("SUBMITTED");
        experiment.setSubmittedAt(LocalDateTime.now());
        experimentMapper.updateById(experiment);
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

    // ---------- 数据导出 ----------

    /** 导出实验记录为 CSV 字符串（含 UTF-8 BOM）。学生只能导出自己的记录。 */
    public String exportCsv(Long taskId, String submitStatus, String role, Long userId) {
        LambdaQueryWrapper<Experiment> wrapper = new LambdaQueryWrapper<>();
        if (role != null && role.contains("STUDENT")) {
            wrapper.eq(Experiment::getStudentId, userId);
        }
        if (taskId != null) {
            wrapper.eq(Experiment::getTaskId, taskId);
        }
        if (submitStatus != null && !submitStatus.isEmpty()) {
            wrapper.eq(Experiment::getSubmitStatus, submitStatus);
        } else if (role == null || !role.contains("STUDENT")) {
            // 教师/管理员默认排除学生草稿（只看到已提交/已批阅）
            wrapper.ne(Experiment::getSubmitStatus, "DRAFT");
        }
        wrapper.orderByDesc(Experiment::getCreatedAt);
        List<Experiment> experiments = experimentMapper.selectList(wrapper);

        // 学生姓名映射
        List<Long> studentIds = experiments.stream()
                .map(Experiment::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            userMapper.selectBatchIds(studentIds).forEach(u -> nameMap.put(u.getId(), u.getRealName()));
        }

        // 评分/评语映射（review 按 experimentId 唯一）
        List<Long> experimentIds = experiments.stream()
                .map(Experiment::getId).collect(Collectors.toList());
        Map<Long, Review> reviewMap = new HashMap<>();
        if (!experimentIds.isEmpty()) {
            reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                    .in(Review::getExperimentId, experimentIds))
                    .forEach(r -> reviewMap.put(r.getExperimentId(), r));
        }

        StringBuilder sb = new StringBuilder();
        // 表头
        sb.append("\uFEFF"); // UTF-8 BOM，避免 Excel 打开乱码
        String[] headers = {"序号", "实验名称", "样品名", "学生", "识别状态", "匹配颜色",
                "匹配度(%)", "色相H(°)", "饱和度S", "明度V", "红占比", "紫占比", "蓝占比",
                "提交时间", "评分", "教师评语"};
        sb.append(joinCsv(headers)).append("\r\n");

        int index = 1;
        for (Experiment e : experiments) {
            Review review = reviewMap.get(e.getId());
            String[] row = {
                    String.valueOf(index++),
                    e.getExperimentName(),
                    e.getSampleName(),
                    nameMap.getOrDefault(e.getStudentId(), String.valueOf(e.getStudentId())),
                    statusLabel(e.getRecognitionStatus()),
                    colorLabel(e.getMatchedColor()),
                    num(e.getConfidence()),
                    num(e.getHue()),
                    num(e.getSaturation()),
                    num(e.getBrightness()),
                    num(e.getRedRatio()),
                    num(e.getPurpleRatio()),
                    num(e.getBlueRatio()),
                    e.getSubmittedAt() == null ? "" : e.getSubmittedAt().toString().replace('T', ' '),
                    review == null || review.getScore() == null ? "" : String.valueOf(review.getScore()),
                    review == null || review.getComment() == null ? "" : review.getComment(),
            };
            sb.append(joinCsv(row)).append("\r\n");
        }
        return sb.toString();
    }

    private static String joinCsv(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escapeCsv(fields[i]));
        }
        return sb.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String statusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "IN_PROGRESS" -> "滴定进行中";
            case "NEAR_ENDPOINT" -> "临近终点";
            case "ENDPOINT" -> "滴定终点";
            case "ABNORMAL" -> "颜色异常";
            default -> status;
        };
    }

    private static String colorLabel(String color) {
        if (color == null) return "";
        return switch (color) {
            case "RED" -> "酒红色";
            case "PURPLE" -> "蓝紫色";
            case "BLUE" -> "纯蓝色";
            case "UNKNOWN" -> "未识别";
            default -> color;
        };
    }

    private static String num(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
