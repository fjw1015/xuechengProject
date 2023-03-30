package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author fjw
 * @date 2023/3/15 0:17
 */
@Service
@Slf4j
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {
    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private CourseMarketMapper courseMarketMapper;
    @Autowired
    private CourseCategoryMapper courseCategoryMapper;
    @Autowired
    private TeachplanService teachplanService;
    @Autowired
    private CourseTeacherService courseTeacherService;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(Long companyId, PageParams pageParams, QueryCourseParamsDto courseParamsDto) {
        //拼装查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(courseParamsDto.getCourseName()
        ), CourseBase::getName, courseParamsDto.getCourseName());
        //根据课程审核状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getAuditStatus()),
                CourseBase::getAuditStatus, courseParamsDto.getAuditStatus());
        //按课程发布状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getPublishStatus()),
                CourseBase::getStatus, courseParamsDto.getPublishStatus());
        //按机构id查询
        queryWrapper.eq(CourseBase::getCompanyId, companyId);
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        List<CourseBase> item = pageResult.getRecords();
        long total = pageResult.getTotal();
        return new PageResult<>(item,
                total, pageParams.getPageNo(), pageParams.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto) {
        //course_base表
        CourseBase courseBaseNew = new CourseBase();
        //只要属性名称一致就可以拷贝
        BeanUtils.copyProperties(addCourseDto, courseBaseNew);
        courseBaseNew.setCompanyId(companyId);
        courseBaseNew.setCreateDate(LocalDateTime.now());
        //设置默认未提交 发布状态未发布
        courseBaseNew.setStatus("203001");
        courseBaseNew.setAuditStatus("202002");
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0) {
            throw new RuntimeException("新增课程失败");
        }

        //course_market表
        CourseMarket courseMarketNew = new CourseMarket();
        Long courseId = courseBaseNew.getId();
        //主键为课程的id
        BeanUtils.copyProperties(addCourseDto, courseMarketNew);
        courseMarketNew.setId(courseId);
        int i = saveCourseMarket(courseMarketNew);
        if (i <= 0) {
            throw new RuntimeException("保存课程营销信息失败");
        }
        return getCourseById(courseId);
    }

    private int saveCourseMarket(CourseMarket courseMarketNew) {
        String charge = courseMarketNew.getCharge();
        //参数合法性校验
        if (StringUtils.isEmpty(charge)) {
            throw new RuntimeException("收费规则为空");
        }
        //如果课程收费 价格没有填写抛出异常
        if (charge.equals("201001")) {
            if (courseMarketNew.getPrice() == null || courseMarketNew.getPrice().floatValue() <= 0) {
                throw new RuntimeException("课程的价格不能为空且必须大于0");
            }
        }
        //从数据库查询营销信息 存在更新 不存在就添加
        Long id = courseMarketNew.getId();
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        if (null == courseMarket) {
            return courseMarketMapper.insert(courseMarketNew);
        } else {
            //将新数据拷贝到旧数据
            BeanUtils.copyProperties(courseMarketNew, courseMarket);
            courseMarket.setId(courseMarketNew.getId());
            //更新
            return courseMarketMapper.updateById(courseMarket);
        }
    }

    @Override
    public CourseBaseInfoDto getCourseById(Long courseId) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (null == courseBase) {
            return null;
        }
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }
        //查询分类名称
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(courseCategoryBySt.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());
        return courseBaseInfoDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseBaseInfoDto updateCourse(Long companyId, EditCourseDto editCourseDto) {
        Long courseId = editCourseDto.getId();
        //查询课程信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        //数据合法性校验 非一般校验
        //本机构只能修改本机构的课程
        if (null == courseBase) {
            XueChengException.cast("本课程信息不存在");
        }
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengException.cast("非本机构无法修改本机构数据");
        }
        BeanUtils.copyProperties(editCourseDto, courseBase);
        //修改时间
        courseBase.setChangeDate(LocalDateTime.now());
        int i = courseBaseMapper.updateById(courseBase);
        if (i <= 0) {
            XueChengException.cast("课程信息更新失败");
        }
        //更新课程营销信息
        boolean flag = true;
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if (null == courseMarket) {
            courseMarket = new CourseMarket();
            flag = false;
        }
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        int j = flag ? courseMarketMapper.updateById(courseMarket) : courseMarketMapper.insert(courseMarket);
        if (j <= 0) {
            XueChengException.cast("课程营销信息更新失败");
        }
        return getCourseById(courseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long companyId, String id) {
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengException.cast("非本机构无法修改本机构数据");
        }
        int i = courseBaseMapper.deleteById(id);
        if (i <= 0) {
            XueChengException.cast("课程基本信息删除失败");
        }
        int j = courseMarketMapper.deleteById(id);
        if (j <= 0) {
            XueChengException.cast("课程营销信息删除失败");
        }
        //删除课程计划数据
        teachplanService.deleteAllTeachPlan(companyId, courseBase);
        //删除课程老师信息数据
        courseTeacherService.deleteAllTeacherInfo(companyId, courseBase);
    }
}
