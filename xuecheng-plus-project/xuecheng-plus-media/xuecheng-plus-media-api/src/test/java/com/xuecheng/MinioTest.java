package com.xuecheng;

import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.diff.Chunk;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author fjw
 * @date 2023/3/20 18:04
 * @description
 */
public class MinioTest {
    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.5.129:9000/")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    //上传文件
    @Test
    public void upload() {
        try {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("mediafiles")   //确定桶
                    .object("a/te北极孤狼.jpg")//对象名
                    .filename("D:\\Pictures\\Pictures\\img\\北极孤狼.jpg")
                    .contentType("image/jpeg")  //指定文件类型
                    .build();
            minioClient.uploadObject(testbucket);
            System.out.println("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("上传失败");
        }
    }

    @Test
    public void delete() {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket("mediafiles").object("北极孤狼.jpg").build());
            System.out.println("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("删除失败");
        }
    }

    /**
     * 从minio下载
     */
    @Test
    public void getFile() {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("mediafiles").object("北极孤狼.jpg").build();
        try (
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                //指定输出流
                FileOutputStream outputStream = new FileOutputStream(new File("D:\\Pictures\\Pictures\\img\\test.jpg"));
        ) {
            IOUtils.copy(inputStream, outputStream);
            FileInputStream fileInputStream1 = new FileInputStream(new File("D:\\Pictures\\Pictures\\img\\test.jpg"));
            String source_md5 = DigestUtils.md5Hex(fileInputStream1);
            FileInputStream fileInputStream = new FileInputStream(new File("D:\\Pictures\\Pictures\\img\\北极孤狼.jpg"));
            String local_md5 = DigestUtils.md5Hex(fileInputStream);
            if (source_md5.equals(local_md5)) {
                System.out.println("下载成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将分块文件上传到minio
     */
    @Test
    public void uploadChunk() throws Exception {
        for (int i = 0; i < 9; i++) {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("video")   //确定桶
                    .object("chunk/" + i)//对象名
                    .filename("D:\\Desktop\\testFile\\chunk\\" + i)
                    .build();
            minioClient.uploadObject(testbucket);
            System.out.println("上传分块" + i + "成功");
        }

    }

    /**
     * 调用minio接口合并分块
     */
    @Test
    public void testMerge() throws Exception{
        //List<ComposeSource> sources = new ArrayList<>();
        //for (int i = 0; i < 8; i++) {
        //    ComposeSource source = ComposeSource.builder().bucket("video").object("chunk/"+ i).build();
        //    sources.add(source);
        //}
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(9)
                .map(i -> ComposeSource.builder().bucket("video")
                        .object("chunk/" + i).build()).collect(Collectors.toList());
        ComposeObjectArgs build = ComposeObjectArgs.builder()
                .sources(sources)
                .bucket("video")
                .object("merge01.avi")
                .build();
        minioClient.composeObject(build);
    }

    /***
     * 批量清理分块文件
     */
    @Test
    public void test_removeObjects(){
        //合并分块完成将分块文件清除
        List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                .limit(9)
                .map(i -> new DeleteObject("chunk/" + i))
                .collect(Collectors.toList());

        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket("video").objects(deleteObjects).build();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
        results.forEach(r->{
            DeleteError deleteError = null;
            try {
                deleteError = r.get();
                System.out.println(deleteError);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
