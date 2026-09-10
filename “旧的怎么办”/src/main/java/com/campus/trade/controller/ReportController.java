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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final DemoDataService demoDataService;

    public ReportController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(demoDataService.getReports());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody ReportRequest request) {
        return ApiResponse.ok(demoDataService.createReport(
                request.targetType(),
                request.targetId(),
                request.reason()
        ));
    }

    public record ReportRequest(
            @NotBlank String targetType,
            @NotNull Long targetId,
            @NotBlank String reason
    ) {
    }
}
