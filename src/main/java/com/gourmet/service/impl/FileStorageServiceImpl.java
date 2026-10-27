package com.gourmet.service.impl;

import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.result.ResultCode;
import com.gourmet.config.UploadProperties;
import com.gourmet.service.FileStorageService;
import com.gourmet.vo.UploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 本地文件存储实现。
 *
 * <p>文件按「业务类型 / 年 / 月 / 日」分目录存放，文件名使用 UUID，
 * 通过 WebConfig 中配置的静态资源映射对外访问。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif",
            "image/webp", "image/bmp");

    private final UploadProperties properties;

    @Override
    public UploadVO upload(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        long maxBytes = properties.getMaxSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ResultCode.PAYLOAD_TOO_LARGE,
                    "图片不能超过 " + properties.getMaxSizeMb() + "MB");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String ext = getExtension(originalName);
        Set<String> allowed = Arrays.stream(properties.getAllowedExt().split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (!allowed.contains(ext)) {
            throw new BusinessException("仅支持上传 " + String.join("/", allowed) + " 格式的图片");
        }

        String contentType = file.getContentType();
        if (contentType != null && !IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("上传的文件不是有效的图片");
        }

        String category = sanitizeType(type);
        String datePath = LocalDate.now().format(DATE_DIR);
        Path baseDir = Paths.get(properties.getPath()).toAbsolutePath().normalize();
        Path dir = baseDir.resolve(category).resolve(datePath);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = dir.resolve(fileName);

        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("文件保存失败：{}", target, e);
            throw new BusinessException(ResultCode.ERROR, "文件保存失败，请稍后重试");
        }

        String urlPrefix = properties.getUrlPrefix();
        if (!urlPrefix.startsWith("/")) {
            urlPrefix = "/" + urlPrefix;
        }
        String url = urlPrefix + "/" + category + "/" + datePath + "/" + fileName;
        log.info("文件上传成功：{} -> {}", originalName, url);
        return new UploadVO(url, originalName, file.getSize());
    }

    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return "common";
        }
        String cleaned = type.replaceAll("[^a-zA-Z0-9_-]", "");
        return cleaned.isEmpty() ? "common" : cleaned;
    }
}
