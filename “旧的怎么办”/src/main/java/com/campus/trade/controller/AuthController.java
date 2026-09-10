package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final DemoDataService demoDataService;

    public AuthController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping("/nickname-available")
    public ApiResponse<Map<String, Object>> checkNicknameAvailability(@RequestParam @NotBlank String nickname) {
        return ApiResponse.ok(demoDataService.checkNicknameAvailability(nickname));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(demoDataService.registerWebUser(request.nickname(), request.password()));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody PasswordLoginRequest request) {
        return ApiResponse.ok(demoDataService.loginWithPassword(request.account(), request.password(), request.loginType()));
    }

    @PostMapping("/wechat/login")
    public ApiResponse<Map<String, Object>> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.ok(demoDataService.wechatLogin(request.code()));
    }

    @PostMapping("/student/verify")
    public ApiResponse<Map<String, Object>> studentVerify(@Valid @RequestBody VerifyRequest request) {
        return ApiResponse.ok(demoDataService.studentVerify(request.studentNo(), request.campus()));
    }

    public record RegisterRequest(@NotBlank String nickname, @NotBlank String password) {
    }

    public record PasswordLoginRequest(@NotBlank String account, @NotBlank String password, String loginType) {
    }

    public record WechatLoginRequest(@NotBlank String code) {
    }

    public record VerifyRequest(@NotBlank String studentNo, @NotBlank String campus) {
    }
}
