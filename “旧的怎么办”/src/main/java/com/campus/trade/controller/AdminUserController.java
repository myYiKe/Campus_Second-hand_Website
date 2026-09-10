package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final DemoDataService demoDataService;

    public AdminUserController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(demoDataService.getAdminUsers());
    }

    @PatchMapping("/status")
    public ApiResponse<Map<String, Object>> updateStatus(@Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.ok(demoDataService.updateAdminUserStatus(request.userId(), request.status()));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long userId) {
        return ApiResponse.ok(demoDataService.deleteAdminUser(userId));
    }

    public record UserStatusRequest(@NotNull Long userId, @NotBlank String status) {
    }
}
