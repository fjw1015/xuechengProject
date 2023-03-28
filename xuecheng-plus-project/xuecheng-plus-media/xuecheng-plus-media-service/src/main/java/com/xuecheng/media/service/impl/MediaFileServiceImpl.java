package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {
    /**
     * 自己注入自己 创建一个代理对象 解决非代理对象无法使用事务的bug
     */
    @Autowired
    MediaFileService currentProxy;
    @Autowired
    MediaFilesMapper mediaFilesMapper;
    @Autowired
    MinioClient minioClient;
    @Value("${minio.bucket.files}")
    private String bucketFiles;
    @Value("${minio.bucket.video}")
    private String bucketVideo;
    @Autowired
    private MediaProcessMapper mediaProcessMapper;
    
    
    @Override
    public PageResult<MediaFiles> queryMediaFiles(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {
        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(queryMediaParamsDto.getFileType())) {
            queryWrapper.eq(MediaFiles::getFileType, queryMediaParamsDto.getFileType());
        }
        if (StringUtils.isNotBlank(queryMediaParamsDto.getFilename())) {
            queryWrapper.like(MediaFiles::getFilename, queryMediaParamsDto.getFilename());
        }
        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        return new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
    }
    
    /**
     * 获取文件默认存储目录路径 年/月/日
     */
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date()).replace("-", "/") + "/";
    }
    
    /**
     * 获取文件的md5
     */
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fileInputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 根据拓展名获取mimeType
     */
    private String getMimeType(String extension) {
        if (extension == null) {
            extension = "";
        }
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }
    
    /**
     * 文件上传 有网络请求，不便添加事务
     */
    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
        File file = new File(localFilePath);
        if (!file.exists()) {
            XueChengException.cast("文件不存在");
        }
        //获取文件名称
        String filename = uploadFileParamsDto.getFilename();
        //获取文件扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //获取文件mimeType
        String mimeType = getMimeType(extension);
        //获取文件的md5值
        String fileMd5 = getFileMd5(file);
        //获取文件的默认目录
        String defaultFolderPath = getDefaultFolderPath();
        //存储到minio中的对象名(带目录)
        String objectName = defaultFolderPath + fileMd5 + extension;
        //将文件上传到minio
        boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucketFiles, objectName);
        if (!result) {
            XueChengException.cast("上传文件失败");
        }
        //文件大小
        uploadFileParamsDto.setFileSize(file.length());
        //将文件信息存储到数据库 使用代理对象执行入库方法
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucketFiles, objectName);
        if (null == mediaFiles) {
            XueChengException.cast("文件数据入库失败");
        }
        //准备返回数据
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }
    
    /**
     * 将文件信息添加到文件表
     * @param companyId           机构id
     * @param fileMd5             文件md5值
     * @param uploadFileParamsDto 上传文件的信息
     * @param bucket              桶
     * @param objectName          对象名称
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        //文件没有被上传过
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            //拷贝基本信息
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            //审核状态
            mediaFiles.setAuditStatus("002003");
            //状态
            mediaFiles.setStatus("1");
            //保存文件信息数据
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert < 0) {
                log.error("保存文件信息到数据库失败,{}", mediaFiles.toString());
                XueChengException.cast("保存文件信息失败");
                return null;
            }
            log.debug("保存文件信息到数据库成功,{}", mediaFiles.toString());
            //记录待处理的任务
            addWaitingTask(mediaFiles);
            
            
            //向MediaProcess 写入数据
        }
        return mediaFiles;
    }
    
    private void addWaitingTask(MediaFiles mediaFiles) {
        //判断mimeType
        String filename = mediaFiles.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(extension);
        if (StringUtils.isNotBlank(mimeType) && mimeType.equals("video/x-msvideo")) {
            //如果是avi视频 写入待处理任务
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles, mediaProcess);
            mediaProcess.setStatus("1");
            mediaProcess.setCreateDate(LocalDateTime.now());
            //失败次数默认为0
            mediaProcess.setFailCount(0);
            mediaProcessMapper.insert(mediaProcess);
        }
        
    }
    
    
    /**
     * 文件上传到minio
     * @param localFilePath 文件地址
     * @param bucket        桶
     * @param objectName    对象名称
     */
    @Override
    public boolean addMediaFilesToMinIO(String localFilePath, String mimeType, String bucket, String objectName) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .filename(localFilePath)
                    .object(objectName)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("上传文件到minio成功,bucket:{},objectName:{}", bucket, objectName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件出错，bucket:{},objectName:{},错误信息:{}", bucket, objectName, e.getMessage());
        }
        return false;
    }
    
    /**
     * 检查文件是否存在
     * @param fileMd5 文件的md5
     */
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (null != mediaFiles) {
            String bucket = mediaFiles.getBucket();
            String filePath = mediaFiles.getFilePath();
            //如果数据库存在再查询minio
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filePath)
                    .build();
            try {
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                if (null != inputStream) {
                    //文件已存在
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return RestResponse.success(false);
    }
    
    /**
     * 检查分块是否存在
     * @param fileMd5    文件的md5
     * @param chunkIndex 分块序号
     */
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        //根据md5得到分块文件所在目录的路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        
        //如果数据库存在再查询 minio
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucketVideo)
                .object(chunkFileFolderPath + chunkIndex)
                .build();
        //查询远程服务获取到一个流对象
        try {
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            if (inputStream != null) {
                //文件已存在
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //文件不存在
        return RestResponse.success(false);
    }
    
    /**
     * 上传分块到minio
     */
    @Override
    public RestResponse uploadchunk(String filePath, String fileMd5, int chunk) {
        //分块文件路径
        String chunkFilePath = getChunkFileFolderPath(fileMd5) + chunk;
        String mimeType = getMimeType(null);
        boolean b = addMediaFilesToMinIO(filePath, mimeType, bucketVideo, chunkFilePath);
        if (!b) {
            return RestResponse.validfail(false, "上传分块文件失败");
        }
        //上传成功
        return RestResponse.success(true);
    }
    
    /**
     * minio文件合并
     * @param companyId           机构id
     * @param fileMd5             文件md5
     * @param chunkTotal          分块总和
     * @param uploadFileParamsDto 文件信息
     */
    @Override
    public RestResponse mergeChunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //分块文件目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //找到所有分块文件 调用minio的sdk进行文件合并
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i)
                .limit(chunkTotal)
                .map(i -> ComposeSource.builder().bucket(bucketVideo)
                        .object(chunkFileFolderPath.concat(Integer.toString(i)))
                        .build()).collect(Collectors.toList());
        //文件名
        String fileName = uploadFileParamsDto.getFilename();
        //拓展名
        String extension = fileName.substring(fileName.lastIndexOf("."));
        //合并文件路径
        String objectName = getFilePathByMd5(fileMd5, extension);
        ComposeObjectArgs build = ComposeObjectArgs.builder()
                .sources(sources)
                .bucket(bucketVideo)
                .object(objectName) //合并后的文件
                .build();
        try {
            minioClient.composeObject(build);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错，bucket :{},objectNAME:{},错误信息:{}", bucketVideo, objectName, e.getMessage());
            return RestResponse.validfail(false, "合并文件异常");
        }
        //校验合并后的和源文件是否一致 视频上传成功
        //先下载合并后的文件
        File file = downloadFileFromMinIO(bucketVideo, objectName);
        //计算合并后文件md5
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String mergeMd5 = DigestUtils.md5Hex(fileInputStream);
            //比较md5
            if (!fileMd5.equals(mergeMd5)) {
                log.error("校验合并文件md5值不一致，原始 {} 合并 {}", fileMd5, mergeMd5);
                return RestResponse.validfail(false, "文件校验失败");
            }
            //文件大小
            uploadFileParamsDto.setFileSize(file.length());
        } catch (Exception e) {
            return RestResponse.validfail(false, "文件校验失败");
        }
        //文件入库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucketVideo, objectName);
        if (null == mediaFiles) {
            return RestResponse.validfail(false, "文件入库失败");
        }
        //删除分块文件
        clearChunkFiles(chunkFileFolderPath, chunkTotal);
        return RestResponse.success(true);
    }
    
    /**
     * 清除分块文件
     * @param chunkFileFolderPath 分块文件路径
     * @param chunkTotal          分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {
        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList());
            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucketVideo).objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清楚分块文件失败,objectName:{}", deleteError.objectName(), e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清楚分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath, e);
        }
    }
    
    /**
     * 得到分块文件的目录
     */
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }
    
    /**
     * 得到合并后的文件的地址
     * @param fileMd5 文件id即md5值
     * @param fileExt 文件扩展名
     */
    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
    
    /**
     * 从minio下载文件
     * @param bucket     桶
     * @param objectName 对象名称
     * @return 下载后的文件
     */
    @Override
    public File downloadFileFromMinIO(String bucket, String objectName) {
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            GetObjectArgs objectArgs = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build();
            InputStream stream = minioClient.getObject(objectArgs);
            //创建临时文件
            minioFile = File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream, outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}