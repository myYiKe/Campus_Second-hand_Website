package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.DemoDataService;
import com.campus.trade.service.FileStorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final DemoDataService demoDataService;
    private final FileStorageService fileStorageService;

    public ItemController(DemoDataService demoDataService, FileStorageService fileStorageService) {
        this.demoDataService = demoDataService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String campus
    ) {
        return ApiResponse.ok(demoDataService.getItems(keyword, category, campus));
    }

    @GetMapping("/{itemId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long itemId) {
        return ApiResponse.ok(demoDataService.getItemDetail(itemId));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateItemRequest request) {
        return ApiResponse.ok(demoDataService.createItem(
                request.title(),
                request.category(),
                request.price(),
                request.stock(),
                request.description(),
                request.imageUrls()
        ));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        String imageUrl = fileStorageService.storeItemImage(file);
        return ApiResponse.ok(Map.of("imageUrl", imageUrl));
    }

    public record CreateItemRequest(
            @NotBlank String title,
            @NotBlank String category,
            @NotNull BigDecimal price,
            @NotNull @Min(1) Integer stock,
            @NotBlank String description,
            List<String> imageUrls
    ) {
        public List<String> imageUrls() {
            return imageUrls == null ? new ArrayList<>() : imageUrls;
        }
    }
}
