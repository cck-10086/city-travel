package com.citytravel.citytravel.controller;

import com.citytravel.citytravel.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {
    
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS =
            new HashSet<>(Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp"));
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            new HashSet<>(Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp"));

    @Value("${app.upload-dir:${user.dir}/uploads}")
    private String uploadDir;

    private Path uploadRoot;
    
    @PostConstruct
    public void init() {
        try {
            uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录", e);
        }
    }
    
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return Result.error(400, "图片大小不能超过5MB");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return Result.error("文件名无效");
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return Result.error(400, "仅支持 JPG、PNG、GIF 和 WebP 图片");
        }
        String newFilename = UUID.randomUUID().toString() + extension;
        
        Path destination = uploadRoot.resolve(newFilename).normalize();
        if (!destination.startsWith(uploadRoot)) {
            return Result.error(400, "文件名无效");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            return Result.success("上传成功", "/images/" + newFilename);
        } catch (IOException e) {
            return Result.error("文件上传失败");
        }
    }
}
