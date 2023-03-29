package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengException;
import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author fjw
 * @date 2023/3/29 2:18
 * @description 课程发布的任务类
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {
    @Autowired
    CoursePublishService coursePublishService;
    @Autowired
    SearchServiceClient searchServiceClient;
    @Autowired
    CoursePublishMapper coursePublishMapper;

    /**
     * 任务调度
     */
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishHandler() throws Exception {
        //分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("shardIndex=" + shardIndex + ",shardTotal=" + shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex, shardTotal, "course_publish", 30, 60);
    }

    /**
     * 课程发布任务处理
     * 如果过程抛出异常，任务失败
     */
    @Override
    public boolean execute(MqMessage mqMessage) {
        //获取消息相关的业务信息
        String businessKey1 = mqMessage.getBusinessKey1();
        long courseId = Integer.parseInt(businessKey1);
        //课程静态化 上传到minio
        generateCourseHtml(mqMessage, courseId);
        //课程索引 向es写索引数据
        saveCourseIndex(mqMessage, courseId);
        //课程缓存redis
        saveCourseCache(mqMessage, courseId);
        return true;
    }

    /**
     * 生成课程静态化页面并上传至文件系统
     */
    public void generateCourseHtml(MqMessage mqMessage, long courseId) {
        log.info("开始进行课程静态化,课程id:{}", courseId);
        //消息id
        Long id = mqMessage.getId();
        //消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        //消息幂等性处理
        int stageOne = mqMessageService.getStageOne(id);
        if (stageOne > 0) {
            log.info("课程静态化已处理直接返回，课程id:{}", courseId);
            return;
        }
        File file = coursePublishService.generateCourseHtml(courseId);
        if (null == file) {
            XueChengException.cast("生成的静态页面为空");
        }
        //文件上传minio
        coursePublishService.uploadCourseHtml(courseId, file);
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //保存第一阶段状态
        mqMessageService.completedStageOne(id);
    }

    /**
     * 将课程信息缓存至redis
     */
    public void saveCourseCache(MqMessage mqMessage, long courseId) {
        log.info("将课程信息缓存至redis,课程id:{}", courseId);
        //消息id
        Long id = mqMessage.getId();
        //消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        //消息幂等性处理
        int stageThree = mqMessageService.getStageThree(id);
        if (stageThree > 0) {
            log.info("将课程信息缓存至redis已处理直接返回，课程id:{}", courseId);
            return;
        }
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //保存第三阶段状态
        mqMessageService.completedStageThree(id);
    }

    /**
     * 保存课程索引信息
     */
    public void saveCourseIndex(MqMessage mqMessage, long courseId) {
        log.info("保存课程索引信息,课程id:{}", courseId);
        //消息id
        Long id = mqMessage.getId();
        //消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        //消息幂等性处理
        int stageTwo = mqMessageService.getStageTwo(id);
        if (stageTwo > 0) {
            log.info("保存课程索引信息已处理直接返回，课程id:{}", courseId);
            return;
        }
        //查询课程信息 调用搜索服务添加索引接口 课程发布表查
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish, courseIndex);
        Boolean add = searchServiceClient.add(courseIndex);
        if (!add) {
            XueChengException.cast("远程调用服务添加索引异常");
        }
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //保存第二阶段状态
        mqMessageService.completedStageTwo(id);
    }
}
