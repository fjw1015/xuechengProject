package com.xuecheng;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author fjw
 * @date 2023/3/21 15:09
 * @description
 */
public class BigFileTest {

    /**
     * 分块测试
     */
    @Test
    public void testChunk() throws Exception {
        //源文件
        File sourceFile = new File("D:\\Desktop\\testFile\\111.avi");
        //分块文件存储路径
        String chunkFilePath = "D:/Desktop/testFile/chunk/";
        File chunkFolder = new File(chunkFilePath);
        if (!chunkFolder.exists()) {
            chunkFolder.mkdirs();
        }
        //分块文件大小
        int chunkFileSize = 1024 * 1024 * 5;
        //分块文件个数
        long chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunkFileSize);
        //使用流从源文件读取数据 向分块文件写数据
        RandomAccessFile accessFile = new RandomAccessFile(sourceFile, "r");
        byte[] bytes = new byte[1024 * 1024];
        for (long i = 0; i < chunkNum; i++) {
            File chunkFile = new File(chunkFilePath + i);
            //分块文件的写入流
            RandomAccessFile rw = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = accessFile.read(bytes)) != -1) {
                rw.write(bytes, 0, len);
                if (chunkFile.length() >= chunkFileSize) {
                    break;
                }
            }
            rw.close();
        }
        accessFile.close();
    }

    /**
     * 将分块文件进行合并
     */
    @Test
    public void testMerge() throws IOException {
        //分块文件存储路径
        File chunkFolder = new File("D:/Desktop/testFile/chunk/");
        //源文件
        File sourceFile = new File("D:\\Desktop\\testFile\\111.avi");
        File mergeFile = new File("D:/Desktop/testFile/chunk/22.avi");
        if (mergeFile.exists()) {
            mergeFile.delete();
        }
        //取出所有分块文件
        File[] files = chunkFolder.listFiles();
        //创建新的合并文件
        mergeFile.createNewFile();
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;
                return o1.getName().compareTo(o2.getName());
            }
        });
        RandomAccessFile rw = new RandomAccessFile(mergeFile, "rw");
        //指针指向文件顶端
        rw.seek(0);
        byte[] bytes = new byte[1024 * 1024];
        //遍历合并
        for (File chunkFile : fileList) {
            RandomAccessFile rRead = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = rRead.read(bytes)) != -1) {
                rw.write(bytes, 0, len);
            }
            rRead.close();
        }
        rw.close();
        //合并文件完成后对合并文件进行校验
        FileInputStream fileInputStreamMerge = new FileInputStream(mergeFile);
        FileInputStream fileInputStreamSource = new FileInputStream(sourceFile);
        String mergeFileMd5 = DigestUtils.md5Hex(fileInputStreamMerge);
        String sourceFileMd5 = DigestUtils.md5Hex(fileInputStreamSource);
        if (mergeFileMd5.equals(sourceFileMd5)) {
            System.out.println("文件合并完成");
        } else {
            System.out.println("文件合并失败");
        }
    }
}
