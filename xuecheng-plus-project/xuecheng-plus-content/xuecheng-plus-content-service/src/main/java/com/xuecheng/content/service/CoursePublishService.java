package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

import java.io.File;

/**
 * <p>
 * 课程发布 服务类
 * </p>
 *
 * @author fjw
 * @since 2023-03-14
 */
public interface CoursePublishService extends IService<CoursePublish> {
    /**
     * @param courseId 课程id
     * @description 获取课程预览信息
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);

    /**
     * @param courseId 课程id
     * @description 提交审核
     */
    public void commitAudit(Long companyId, Long courseId);

    /**
     * @param companyId 机构id
     * @param courseId  课程id
     * @description 课程发布接口
     */
    public void publish(Long companyId, Long courseId);

    /**
     * @param courseId 课程id
     * @return File 静态化文件
     * @description 课程静态化
     */
    public File generateCourseHtml(Long courseId);

    /**
     * @param file 静态化文件
     * @description 上传课程静态化页面
     */
    public void uploadCourseHtml(Long courseId, File file);

    CoursePublish getCoursePublish(Long courseId);

    /**
     * @description 查询缓存中的课程信息
     */
    public CoursePublish getCoursePublishCache(Long courseId);
}
