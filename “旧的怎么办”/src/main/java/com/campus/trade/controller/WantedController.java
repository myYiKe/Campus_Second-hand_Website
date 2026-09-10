package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
@RequestMapping("/api/wanted")
public class WantedController {
    private final DemoDataService demoDataService;

    public WantedController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(demoDataService.getWantedPosts());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateWantedRequest request) {
        return ApiResponse.ok(demoDataService.createWanted(
                request.title(),
                request.budget(),
                request.description()
        ));
    }

    public record CreateWantedRequest(
            @NotBlank String title,
            @NotNull BigDecimal budget,
            @NotBlank String description
    ) {
    }
}
