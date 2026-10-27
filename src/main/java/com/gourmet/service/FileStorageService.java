package com.gourmet.service;

import com.gourmet.vo.UploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传。
 */
public interface FileStorageService {

    /**
     * 保存图片并返回可访问地址。
     *
     * @param file 上传文件
     * @param type 业务类型：avatar / dish / review / note / merchant
     */
    UploadVO upload(MultipartFile file, String type);
}
