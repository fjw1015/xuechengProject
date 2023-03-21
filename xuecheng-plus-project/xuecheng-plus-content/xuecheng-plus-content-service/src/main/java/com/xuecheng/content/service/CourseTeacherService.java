package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.CourseTeacherDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

/**
 * <p>
 * 课程-教师关系表 服务类
 * </p>
 *
 * @author fjw
 * @since 2023-03-14
 */
public interface CourseTeacherService extends IService<CourseTeacher> {

    List<CourseTeacherDto> teacherList(Long id);

    void saveTeacherInfo(Long companyId, CourseTeacherDto courseTeacherDto);

    void deleteTeacherInfo(Long companyId, String courseId, String id);

    void deleteAllTeacherInfo(Long companyId, CourseBase courseId);
}
