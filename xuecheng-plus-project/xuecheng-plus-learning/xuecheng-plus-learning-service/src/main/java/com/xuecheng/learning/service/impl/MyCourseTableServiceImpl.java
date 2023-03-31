package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengException;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author fjw
 * @date 2023/3/31 2:34
 * @description
 */
@Service
@Slf4j
public class MyCourseTableServiceImpl implements MyCourseTableService {

    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;
    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;
    @Autowired
    ContentServiceClient contentServiceClient;
    @Autowired
    MyCourseTableService myCourseTableService;
    @Autowired
    MyCourseTableServiceImpl currentProxy;
    @Autowired
    XcChooseCourseMapper chooseCourseMapper;

    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        //课程收费标准
        String charge = coursepublish.getCharge();
        //选课记录
        XcChooseCourse chooseCourse = null;
        if ("201000".equals(charge)) {
            //添加免费课程
            chooseCourse = addFreeCoruse(userId, coursepublish);
            //添加到我的课程表
            XcCourseTables xcCourseTables = addCourseTables(chooseCourse);
        } else {
            //添加收费课程
            chooseCourse = addChargeCoruse(userId, coursepublish);
        }
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse, xcChooseCourseDto);
        //获取学习资格
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());
        return xcChooseCourseDto;
    }

    /**
     * 添加免费课程,免费课程加入选课记录表、我的课程表
     */
    public XcChooseCourse addFreeCoruse(String userId, CoursePublish coursepublish) {
        //查询选课记录表是否存在免费的且选课成功的订单
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700001")//免费课程
                .eq(XcChooseCourse::getStatus, "701001");//选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        //如果选过了 直接返回
        if (xcChooseCourses != null && xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }
        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(0f); //免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");  //免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001"); //选课成功
        xcChooseCourse.setValidDays(365);   //免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now()); //开始时间
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));  //结束时间
        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0) {
            XueChengException.cast("添加选课记录失败");
        }
        return xcChooseCourse;
    }

    /**
     * 添加收费课程
     */
    public XcChooseCourse addChargeCoruse(String userId, CoursePublish coursepublish) {
        //如果存在待支付记录直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700002")//收费订单
                .eq(XcChooseCourse::getStatus, "701002");//待支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");//收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701002");//待支付
        xcChooseCourse.setValidDays(coursepublish.getValidDays());
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursepublish.getValidDays()));
        xcChooseCourseMapper.insert(xcChooseCourse);
        return xcChooseCourse;
    }

    /**
     * @param xcChooseCourse 选课记录
     * @description 添加到我的课程表
     */
    public XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse) {
        //选课记录完成且未过期可以添加课程到课程表
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)) {
            XueChengException.cast("选课未成功，无法添加到课程表");
        }
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if (xcCourseTables != null) {
            return xcCourseTables;
        }
        XcCourseTables xcCourseTablesNew = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse, xcCourseTablesNew);
        xcCourseTablesNew.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTablesNew.setCreateDate(LocalDateTime.now());
        xcCourseTablesNew.setCourseType(xcChooseCourse.getOrderType());
        int insert = xcCourseTablesMapper.insert(xcCourseTablesNew);
        if (insert <= 0) {
            XueChengException.cast("添加课程表失败");
        }
        return xcCourseTablesNew;
    }

    /**
     * @description 根据课程和用户查询我的课程表中某一门课程
     * @author fjw
     */
    public XcCourseTables getXcCourseTables(String userId, Long courseId) {
        LambdaQueryWrapper<XcCourseTables> queryWrapper = new LambdaQueryWrapper<XcCourseTables>()
                .eq(XcCourseTables::getUserId, userId)
                .eq(XcCourseTables::getCourseId, courseId);
        return xcCourseTablesMapper.selectOne(queryWrapper);
    }

    /**
     * @return XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     * @description 判断学习资格
     */
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if (xcCourseTables == null) {
            //没有选课或选课后没有支付
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        BeanUtils.copyProperties(xcCourseTables, xcCourseTablesDto);
        //是否过期,true过期，false未过期
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (!isExpires) {
            //正常学习
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;
        } else {
            //已过期
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }
    }

    @Transactional
    @Override
    public boolean saveChooseCourseSuccess(String chooseCourseId) {
        //根据choosecourseId查询选课记录
        XcChooseCourse xcChooseCourse = chooseCourseMapper.selectById(chooseCourseId);
        if (xcChooseCourse == null) {
            log.info("收到支付结果通知但没有查询到关联的选课记录,choosecourseId:{}", chooseCourseId);
            return false;
        }
        String status = xcChooseCourse.getStatus();
        if ("701001".equals(status)) {
            //添加到课程表
            addCourseTables(xcChooseCourse);
            return true;
        }
        //只要当支付时才更新为已支付
        if ("701002".equals(status)) {
            //更新选课记录的状态为选课成功
            xcChooseCourse.setStatus("701001");
            int update = chooseCourseMapper.updateById(xcChooseCourse);
            if (update > 0) {
                log.info("收到支付结果通知处理成功,选课记录:{}", xcChooseCourse);
                //添加到课程表
                addCourseTables(xcChooseCourse);
                return true;
            } else {
                log.info("收到支付结果通知处理失败,选课记录:{}", xcChooseCourse);
                return false;
            }
        }
        return false;
    }
}
