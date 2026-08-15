package com.example.titration.module.review.controller;

import com.example.titration.common.result.R;
import com.example.titration.module.log.annotation.OperationLog;
import com.example.titration.module.review.entity.Review;
import com.example.titration.module.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "批阅评分", description = "教师批阅评分接口")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "获取批阅列表")
    public R<Object> list(@RequestParam(required = false) String status,
                          @RequestParam(required = false) Long experimentId) {
        return R.ok(reviewService.listReviews(status, experimentId));
    }

    @PostMapping
    @Operation(summary = "提交批阅评分")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @OperationLog(value = "批阅评分", content = "提交实验批阅")
    public R<Review> create(@RequestBody Review review) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return R.ok("批阅成功", reviewService.createReview(review, (Long) auth.getPrincipal()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新批阅评分")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public R<Review> update(@PathVariable Long id, @RequestBody Review review) {
        return R.ok("更新成功", reviewService.updateReview(id, review));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除批阅")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return R.ok("删除成功");
    }
}
