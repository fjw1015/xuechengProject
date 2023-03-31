package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fjw
 * @date 2023/3/28 16:40
 * @description
 */
@Api(value = "课程公开查询接口", tags = "课程公开查询接口")
@RestController
public class CourseOpenController {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;
    @Autowired
    private CoursePublishService coursePublishService;

    @GetMapping("/open/course/whole/{courseId}")
    public CoursePreviewDto getPreviewInfo(@PathVariable("courseId") Long courseId) {
        //获取课程预览信息
        return coursePublishService.getCoursePreviewInfo(courseId);
    }

    @GetMapping("/course/whole/{courseId}")
    public CoursePreviewDto getPreviewInfo2(@PathVariable("courseId") Long courseId) {
        //获取课程预览信息
        return coursePublishService.getCoursePreviewInfo(courseId);
    }

}
