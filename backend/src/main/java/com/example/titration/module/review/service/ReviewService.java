package com.example.titration.module.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.titration.common.exception.BusinessException;
import com.example.titration.module.experiment.entity.Experiment;
import com.example.titration.module.experiment.mapper.ExperimentMapper;
import com.example.titration.module.review.entity.Review;
import com.example.titration.module.review.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final ExperimentMapper experimentMapper;

    public List<Review> listReviews(String status) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Review::getStatus, status);
        }
        wrapper.orderByDesc(Review::getCreatedAt);
        return reviewMapper.selectList(wrapper);
    }

    @Transactional
    public Review createReview(Review review, Long teacherId) {
        Review existing = reviewMapper.selectOne(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getExperimentId, review.getExperimentId()));
        if (existing != null) {
            throw new BusinessException(400, "该实验记录已有批阅");
        }
        review.setTeacherId(teacherId);
        review.setReviewedAt(LocalDateTime.now());
        review.setStatus("REVIEWED");
        reviewMapper.insert(review);

        Experiment experiment = experimentMapper.selectById(review.getExperimentId());
        if (experiment != null) {
            experiment.setSubmitStatus("REVIEWED");
            experimentMapper.updateById(experiment);
        }
        return review;
    }

    public Review updateReview(Long id, Review review) {
        if (reviewMapper.selectById(id) == null) {
            throw new BusinessException(404, "批阅记录不存在");
        }
        review.setId(id);
        reviewMapper.updateById(review);
        return reviewMapper.selectById(id);
    }

    public void deleteReview(Long id) {
        if (reviewMapper.deleteById(id) == 0) {
            throw new BusinessException(404, "批阅记录不存在");
        }
    }
}
