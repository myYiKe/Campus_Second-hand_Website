package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final DemoDataService demoDataService;

    public OrderController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(demoDataService.getOrders(status));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(demoDataService.createOrder(
                request.itemId(),
                request.receiverName(),
                request.receiverPhone(),
                request.receiverCampus(),
                request.receiverDetail()
        ));
    }

    @PatchMapping("/pay")
    public ApiResponse<Map<String, Object>> pay(@Valid @RequestBody PayOrderRequest request) {
        return ApiResponse.ok(demoDataService.payOrder(request.orderId(), request.paymentMethod()));
    }

    @PatchMapping("/ship")
    public ApiResponse<Map<String, Object>> ship(@Valid @RequestBody OrderActionRequest request) {
        return ApiResponse.ok(demoDataService.shipOrder(request.orderId()));
    }

    @PatchMapping("/complete")
    public ApiResponse<Map<String, Object>> complete(@Valid @RequestBody OrderActionRequest request) {
        return ApiResponse.ok(demoDataService.completeOrder(request.orderId()));
    }

    @PatchMapping("/cancel")
    public ApiResponse<Map<String, Object>> cancel(@Valid @RequestBody CancelOrderRequest request) {
        return ApiResponse.ok(demoDataService.cancelOrder(request.orderId(), request.cancelReason()));
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Map<String, Object>> hide(@PathVariable Long orderId) {
        return ApiResponse.ok(demoDataService.hideOrderRecord(orderId));
    }

    public record CreateOrderRequest(
            @NotNull Long itemId,
            @NotBlank String receiverName,
            @NotBlank String receiverPhone,
            @NotBlank String receiverCampus,
            @NotBlank String receiverDetail
    ) {
    }

    public record PayOrderRequest(@NotNull Long orderId, @NotBlank String paymentMethod) {
    }

    public record OrderActionRequest(@NotNull Long orderId) {
    }

    public record CancelOrderRequest(@NotNull Long orderId, @NotBlank String cancelReason) {
    }
}
