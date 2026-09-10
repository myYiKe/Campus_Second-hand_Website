package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/my/items")
public class UserItemController {
    private final DemoDataService demoDataService;

    public UserItemController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(demoDataService.getMyItems());
    }

    @PatchMapping("/{itemId}/status")
    public ApiResponse<Map<String, Object>> updateStatus(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemStatusRequest request
    ) {
        return ApiResponse.ok(demoDataService.updateMyItemStatus(itemId, request.status()));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long itemId) {
        return ApiResponse.ok(demoDataService.deleteMyItem(itemId));
    }

    public record UpdateItemStatusRequest(@NotBlank String status) {
    }
}
