package com.campus.trade.service;

import com.campus.trade.common.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_SUFFIXES = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String storeItemImage(MultipartFile file) {
        return storeImage(file);
    }

    public String storeAvatarImage(MultipartFile file) {
        return storeImage(file);
    }

    private String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "请选择要上传的图片");
        }
        String originalName = file.getOriginalFilename();
        String suffix = extractSuffix(originalName);
        if (!ALLOWED_SUFFIXES.contains(suffix)) {
            throw new BusinessException("FILE_TYPE_NOT_ALLOWED", "仅支持 jpg、jpeg、png、webp 图片");
        }
        try {
            Files.createDirectories(uploadDir);
            String fileName = UUID.randomUUID() + suffix;
            Path target = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + fileName;
        } catch (IOException exception) {
            throw new BusinessException("FILE_UPLOAD_FAILED", "图片上传失败");
        }
    }

    private String extractSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
    }
}
