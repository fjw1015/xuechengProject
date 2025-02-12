package com.xuecheng.media.api;

import com.xuecheng.base.exception.XueChengException;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fjw
 * @date 2023/3/28 16:41
 * @description
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController
@RequestMapping("/open")
public class MediaOpenController {

    @Autowired
    MediaFileService mediaFileService;

    @ApiOperation("预览文件")
    @GetMapping("/preview/{mediaId}")
    public RestResponse<String> getPlayUrlByMediaId(@PathVariable String mediaId) {
        MediaFiles mediaFiles = mediaFileService.getFileById(mediaId);
        if (null == mediaFiles) {
            return RestResponse.validfail("找不到视频");
        }
        String url = mediaFiles.getUrl();
        if (StringUtils.isEmpty(url)) {
            XueChengException.cast("视频正在处理中");
        }
        return RestResponse.success(url);
    }
}