package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author fjw
 * @date 2023/3/29 20:56
 * @description
 */
@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {
    @Override
    public MediaServiceClient create(Throwable cause) {
        //发生熔断 上传服务调用此方法执行降级
        return new MediaServiceClient() {
            @Override
            public String uploadFile(MultipartFile filedata, String objectName) throws IOException {
                log.info("远程调用上传文件的接口发生的异常:{}", cause.toString(), cause);
                return null;
            }
        };
    }
}
