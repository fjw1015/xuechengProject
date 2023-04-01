package com.xuecheng.learning.service;

import com.xuecheng.base.model.RestResponse;

/**
 * @author fjw
 * @version 1.0
 * @description 在线学习相关的接口
 */
public interface LearningService {

    /**
     * @param courseId    课程id
     * @param teachplanId 课程计划id
     * @param mediaId     视频文件id
     * @description 获取教学视频
     */
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId);

}
