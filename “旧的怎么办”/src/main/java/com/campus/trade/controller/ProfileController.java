package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import com.campus.trade.service.FileStorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final DemoDataService demoDataService;
    private final FileStorageService fileStorageService;

    public ProfileController(DemoDataService demoDataService, FileStorageService fileStorageService) {
        this.demoDataService = demoDataService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getProfile() {
        return ApiResponse.ok(demoDataService.getUserProfile());
    }

    @PatchMapping("/nickname")
    public ApiResponse<Map<String, Object>> updateNickname(@Valid @RequestBody NicknameRequest request) {
        return ApiResponse.ok(demoDataService.updateNickname(request.nickname()));
    }

    @PatchMapping("/password")
    public ApiResponse<Map<String, Object>> updatePassword(@Valid @RequestBody PasswordRequest request) {
        return ApiResponse.ok(demoDataService.updatePassword(request.oldPassword(), request.newPassword()));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(demoDataService.updateAvatar(fileStorageService.storeAvatarImage(file)));
    }

    public record NicknameRequest(@NotBlank String nickname) {
    }

    public record PasswordRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {
    }
}
