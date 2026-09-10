package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final DemoDataService demoDataService;

    public AdminController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(demoDataService.getDashboard());
    }

    @PatchMapping("/items/audit")
    public ApiResponse<Map<String, Object>> auditItem(@Valid @RequestBody AuditRequest request) {
        return ApiResponse.ok(demoDataService.auditItem(
                request.itemId(),
                request.result(),
                request.reason(),
                request.userAction()
        ));
    }

    public record AuditRequest(@NotNull Long itemId, @NotBlank String result, String reason, String userAction) {
    }
}
