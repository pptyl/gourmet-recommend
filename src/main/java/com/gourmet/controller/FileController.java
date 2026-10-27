package com.gourmet.controller;

import com.gourmet.common.result.Result;
import com.gourmet.service.FileStorageService;
import com.gourmet.vo.UploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传：头像、菜品图、评价图、笔记图、店铺封面。
 */
@Tag(name = "14-文件上传", description = "统一图片上传接口")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传图片", description = "type 取值：avatar / dish / review / note / merchant / common")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadVO> upload(@Parameter(description = "图片文件")
                                   @RequestParam("file") MultipartFile file,
                                   @Parameter(description = "业务类型，用于分目录存放")
                                   @RequestParam(defaultValue = "common") String type) {
        return Result.ok("上传成功", fileStorageService.upload(file, type));
    }
}
