package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/disputes")
public class DisputeController {
    private final DemoDataService demoDataService;

    public DisputeController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(demoDataService.getDisputes());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateDisputeRequest request) {
        return ApiResponse.ok(demoDataService.createDispute(request.orderId(), request.reason()));
    }

    @PatchMapping("/handle")
    public ApiResponse<Map<String, Object>> handle(@Valid @RequestBody HandleDisputeRequest request) {
        return ApiResponse.ok(demoDataService.handleDispute(
                request.disputeId(),
                request.status(),
                request.handleResult()
        ));
    }

    public record CreateDisputeRequest(@NotNull Long orderId, @NotBlank String reason) {
    }

    public record HandleDisputeRequest(
            @NotNull Long disputeId,
            @NotBlank String status,
            @NotBlank String handleResult
    ) {
    }
}
