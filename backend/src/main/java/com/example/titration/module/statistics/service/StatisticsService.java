package com.example.titration.module.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.titration.module.experiment.entity.Experiment;
import com.example.titration.module.experiment.mapper.ExperimentMapper;
import com.example.titration.module.review.entity.Review;
import com.example.titration.module.review.mapper.ReviewMapper;
import com.example.titration.module.task.entity.ExperimentTask;
import com.example.titration.module.task.mapper.ExperimentTaskMapper;
import com.example.titration.module.user.entity.User;
import com.example.titration.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserMapper userMapper;
    private final ExperimentTaskMapper taskMapper;
    private final ExperimentMapper experimentMapper;
    private final ReviewMapper reviewMapper;

    public Map<String, Object> getOverview() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalStudents", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "STUDENT")));

        stats.put("totalTasks", taskMapper.selectCount(
                new LambdaQueryWrapper<ExperimentTask>().eq(ExperimentTask::getStatus, "PUBLISHED")));

        stats.put("totalExperiments", experimentMapper.selectCount(null));

        Long submittedCount = experimentMapper.selectCount(
                new LambdaQueryWrapper<Experiment>().eq(Experiment::getSubmitStatus, "SUBMITTED"));
        stats.put("submittedCount", submittedCount);

        Long reviewedCount = experimentMapper.selectCount(
                new LambdaQueryWrapper<Experiment>().eq(Experiment::getSubmitStatus, "REVIEWED"));
        stats.put("reviewedCount", reviewedCount);

        stats.put("pendingReviewCount", submittedCount - reviewedCount);

        for (String status : List.of("IN_PROGRESS", "NEAR_ENDPOINT", "ENDPOINT", "ABNORMAL")) {
            Long count = experimentMapper.selectCount(
                    new LambdaQueryWrapper<Experiment>().eq(Experiment::getRecognitionStatus, status));
            stats.put(status.toLowerCase() + "Count", count);
        }

        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().isNotNull(Review::getScore));
        if (!reviews.isEmpty()) {
            BigDecimal avgScore = reviews.stream()
                    .map(Review::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);
            stats.put("averageScore", avgScore);
        } else {
            stats.put("averageScore", 0);
        }
        return stats;
    }
}
