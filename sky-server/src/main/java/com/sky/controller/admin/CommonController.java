package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliyunOSSOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@RestController
@RequestMapping("/admin/common")
public class CommonController {
    @Autowired
    AliyunOSSOperator aliyunOSSOperator;

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);

        String url = null;
        try {
            url = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
        } catch (Exception e) {
            //上传失败
            log.error("文件上传失败", e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
        log.info("文件上传成功，{}", url);

        return Result.success(url);
    }
}
