package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final DemoDataService demoDataService;

    public FavoriteController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(demoDataService.getFavorites());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody FavoriteRequest request) {
        return ApiResponse.ok(demoDataService.addFavorite(request.itemId()));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Map<String, Object>> remove(@PathVariable Long itemId) {
        return ApiResponse.ok(demoDataService.removeFavorite(itemId));
    }

    public record FavoriteRequest(@NotNull Long itemId) {
    }
}
