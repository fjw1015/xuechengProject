package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author fjw
 * @version 1.0
 * @description 在线学习接口
 */
@Slf4j
@Service
public class LearningServiceImpl implements LearningService {
    @Autowired
    MyCourseTableService myCourseTablesService;
    @Autowired
    ContentServiceClient contentServiceClient;
    @Autowired
    MediaServiceClient mediaServiceClient;

    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        //判断如果为null不再继续
        if (coursepublish == null) {
            return RestResponse.validfail("课程不存在");
        }
        //从coursepublish对象中解析出课程计划信息去判断是否支持试学
        //如果支持试学调用媒资服务查询视频的播放地址，返回
        String teachPlan = coursepublish.getTeachplan();
        List<TeachPlanDto> teachPlanDtos = JSON.parseArray(teachPlan, TeachPlanDto.class);
        for (TeachPlanDto teachPlanDto : teachPlanDtos) {
            List<TeachPlanDto> teachPlanTreeNodes = teachPlanDto.getTeachPlanTreeNodes();
            for (TeachPlanDto teachplan : teachPlanTreeNodes) {
                // 如果is_preview的值为1表示支持试学
                String isPreview = teachplan.getIsPreview();
                if (StringUtils.isNotEmpty(isPreview) && "1".equals(isPreview)) {
                    return mediaServiceClient.getPlayUrlByMediaId(mediaId);
                }
            }
        }
        //判断用户是否已登录
        if (StringUtils.isNotEmpty(userId)) {
            //获取学习资格
            XcCourseTablesDto learningStatus = myCourseTablesService.getLearningStatus(userId, courseId);
            //学习资格，[{"code":"702001","desc":"正常学习"},
            // {"code":"702002","desc":"没有选课或选课后没有支付"},
            // {"code":"702003","desc":"已过期需要申请续期或重新支付"}]
            String learnStatus = learningStatus.getLearnStatus();
            if ("702002".equals(learnStatus)) {
                return RestResponse.validfail("无法学习，因为没有选课或选课后没有支付");
            } else if ("702003".equals(learnStatus)) {
                return RestResponse.validfail("已过期需要申请续期或重新支付");
            } else {
                //有资格学习，要返回视频的播放地址
                //远程程调用媒资获取视频播放地址
                return mediaServiceClient.getPlayUrlByMediaId(mediaId);
            }
        }
        //如果用户没有登录
        //取出课程的收费规则
        String charge = coursepublish.getCharge();
        if ("201000".equals(charge)) {
            //有资格学习，要返回视频的播放地址
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }
        return RestResponse.validfail("课程未购买，请购买之后再学习");
    }
}
