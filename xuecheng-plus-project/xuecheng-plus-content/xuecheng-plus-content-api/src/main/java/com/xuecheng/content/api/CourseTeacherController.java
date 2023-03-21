package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseTeacherDto;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author fjw
 * @date 2023/3/17 0:59
 * @description
 */
@RestController
@Api("教师设置管理")
public class CourseTeacherController {
    @Autowired
    private CourseTeacherService courseTeacherService;

    @ApiOperation("课程教师信息")
    @GetMapping("/courseTeacher/list/{id}")
    public List<CourseTeacherDto> teacherList(@PathVariable Long id) {
        return courseTeacherService.teacherList(id);
    }

    @ApiOperation("修改或添加课程教师信息")
    @PostMapping("/courseTeacher")
    public void saveTeacherInfo(@RequestBody CourseTeacherDto courseTeacherDto) {
        courseTeacherService.saveTeacherInfo(1232141425L,courseTeacherDto);
    }

    @ApiOperation("课程计划数据更新和添加")
    @DeleteMapping("/courseTeacher/course/{courseId}/{id}")
    public void deleteTeacherInfo(@PathVariable String courseId, @PathVariable String id) {
        courseTeacherService.deleteTeacherInfo(1232141425L,courseId,id);
    }
}
