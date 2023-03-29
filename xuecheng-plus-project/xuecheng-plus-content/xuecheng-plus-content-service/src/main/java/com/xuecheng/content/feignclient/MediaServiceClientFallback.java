package com.xuecheng.content.feignclient;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author fjw
 * @date 2023/3/29 20:54
 * @description
 */
public class MediaServiceClientFallback implements MediaServiceClient {
    @Override
    public String uploadFile(MultipartFile filedata, String objectName) throws IOException {

        return null;
    }
}
