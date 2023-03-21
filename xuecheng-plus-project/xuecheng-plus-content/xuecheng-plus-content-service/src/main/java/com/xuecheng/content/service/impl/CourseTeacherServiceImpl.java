package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.dto.CourseTeacherDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 课程-教师关系表 服务实现类
 * </p>
 *
 * @author fjw
 */
@Slf4j
@Service
public class CourseTeacherServiceImpl extends ServiceImpl<CourseTeacherMapper, CourseTeacher> implements CourseTeacherService {
    @Autowired
    private CourseTeacherMapper courseTeacherMapper;
    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Override
    public List<CourseTeacherDto> teacherList(Long id) {
        List<CourseTeacherDto> result = new ArrayList<>();
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, id);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(queryWrapper);
        courseTeachers.forEach(courseTeacher -> {
            CourseTeacherDto courseTeacherDto = new CourseTeacherDto();
            BeanUtils.copyProperties(courseTeacher, courseTeacherDto);
            result.add(courseTeacherDto);
        });
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTeacherInfo(Long companyId, CourseTeacherDto courseTeacherDto) {
        Long courseId = courseTeacherDto.getCourseId();
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengException.cast("非本机构无法修改本机构数据");
        }
        Long id = courseTeacherDto.getId();
        CourseTeacher courseTeacher = courseTeacherMapper.selectById(id);
        if (null != courseTeacher) {
            BeanUtils.copyProperties(courseTeacherDto, courseTeacher);
            int i = courseTeacherMapper.updateById(courseTeacher);
            if (i <= 0) {
                XueChengException.cast("教师信息修改失败");
            }
        } else {
            courseTeacher = new CourseTeacher();
            BeanUtils.copyProperties(courseTeacherDto, courseTeacher);
            int i = courseTeacherMapper.insert(courseTeacher);
            if (i <= 0) {
                XueChengException.cast("教师信息添加失败");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTeacherInfo(Long companyId, String courseId, String id) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengException.cast("非本机构无法修改本机构数据");
        }
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        queryWrapper.eq(CourseTeacher::getId, id);
        int result = courseTeacherMapper.delete(queryWrapper);
        if (result <= 0) {
            XueChengException.cast("删除教师信息失败");
        }
    }

    /**
     * 用于其他接口进行调用删除，删除该课程下所有的教师信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllTeacherInfo(Long companyId, CourseBase courseBase) {
        Long courseId = courseBase.getId();
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengException.cast("非本机构无法修改本机构数据");
        }
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        int result = courseTeacherMapper.delete(queryWrapper);
        if (result <= 0) {
            XueChengException.cast("删除教师信息失败");
        }
    }
}
