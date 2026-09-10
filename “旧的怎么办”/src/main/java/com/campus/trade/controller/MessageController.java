package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/messages")
public class MessageController {
    private final DemoDataService demoDataService;

    public MessageController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long itemId
    ) {
        return ApiResponse.ok(demoDataService.getMessages(orderId, itemId));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> send(@Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(demoDataService.sendMessage(
                request.orderId(),
                request.itemId(),
                request.toUserId(),
                request.content()
        ));
    }

    @DeleteMapping("/thread")
    public ApiResponse<Map<String, Object>> hide(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long itemId
    ) {
        return ApiResponse.ok(demoDataService.hideMessageThread(orderId, itemId));
    }

    public record SendMessageRequest(
            Long orderId,
            Long itemId,
            Long toUserId,
            @NotBlank String content
    ) {
    }
}
