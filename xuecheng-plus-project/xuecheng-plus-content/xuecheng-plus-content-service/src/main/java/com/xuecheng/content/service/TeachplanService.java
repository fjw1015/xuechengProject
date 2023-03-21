package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.SaveTeachPlanDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.Teachplan;

import java.util.List;

/**
 * <p>
 * 课程计划 服务类
 * </p>
 *
 * @author fjw
 * @since 2023-03-14
 */
public interface TeachplanService extends IService<Teachplan> {

    List<TeachPlanDto> getTreeNodes(Long courseId);

    void saveTeachPlan(SaveTeachPlanDto saveTeachPlanDto);

    void deleteTeachPlan(Long id);

    void deleteAllTeachPlan(Long companyId, CourseBase courseId);

    void move(String type, Long id);
}
