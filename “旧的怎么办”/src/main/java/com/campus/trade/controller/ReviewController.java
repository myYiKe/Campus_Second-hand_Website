package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final DemoDataService demoDataService;

    public ReviewController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(demoDataService.getReviews());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.ok(demoDataService.createReview(
                request.orderId(),
                request.score(),
                request.content()
        ));
    }

    public record CreateReviewRequest(
            @NotNull Long orderId,
            @Min(1) @Max(5) int score,
            @NotBlank String content
    ) {
    }
}
