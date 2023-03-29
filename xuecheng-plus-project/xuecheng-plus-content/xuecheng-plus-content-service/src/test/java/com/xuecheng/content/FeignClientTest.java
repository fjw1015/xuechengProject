package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author fjw
 * @date 2023/3/29 15:44
 * @description
 */
@SpringBootTest
public class FeignClientTest {
    @Autowired
    MediaServiceClient mediaServiceClient;
    
    //远程调用，上传文件
    @Test
    public void test() throws IOException {
        
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(new File("D:\\desktop\\myproject\\test.html"));
        mediaServiceClient.uploadFile(multipartFile, "course/test.html");
    }
}

