package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachPlanDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 课程计划 服务实现类
 * </p>
 *
 * @author fjw
 */
@Slf4j
@Service
public class TeachplanServiceImpl extends ServiceImpl<TeachplanMapper, Teachplan> implements TeachplanService {
    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;
    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Override
    public List<TeachPlanDto> getTreeNodes(Long courseId) {
        return teachplanMapper.getTreeNodes(courseId);
    }

    /**
     * 添加修改课程计划
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTeachPlan(SaveTeachPlanDto saveTeachPlanDto) {
        Long id = saveTeachPlanDto.getId();
        //添加小结
        if (null != id) {
            //修改课程计划
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(saveTeachPlanDto, teachplan);
            teachplanMapper.updateById(teachplan);
        } else {
            //新增课程计划 确定排序位置
            Teachplan teachplanNew = new Teachplan();
            int count = getTeachPlanCount(saveTeachPlanDto.getCourseId(), saveTeachPlanDto.getParentid());
            teachplanNew.setOrderby(count + 1);
            BeanUtils.copyProperties(saveTeachPlanDto, teachplanNew);
            teachplanMapper.insert(teachplanNew);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTeachPlan(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        Integer grade = teachplan.getGrade();
        if (grade == 2) {
            //删除小结
            TeachplanMedia teachplanMedia = teachplanMediaMapper.selectByTeachPlanId(id);
            if (null != teachplanMedia) {
                int j = teachplanMediaMapper.deleteByTeachplanId(id);
                if (j <= 0) {
                    XueChengException.cast("删除视频数据失败");
                }
            }
            int i = teachplanMapper.deleteById(id);
            if (i <= 0) {
                XueChengException.cast("删除小结数据失败");
            }
        } else if (grade == 1) {
            //删除章节
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid, id);
            int i = teachplanMapper.selectCount(queryWrapper);
            if (i > 0) {
                XueChengException.cast("课程计划信息还有子级信息，无法操作");
            }
            int j = teachplanMapper.deleteById(id);
            if (j <= 0) {
                XueChengException.cast("删除章节信息失败");
            }
        }
    }

    /**
     * 用于其他接口调用的删除方法 删除同企业和课程id的索引课程计划信息
     * 信息包括课程计划和课程媒体信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllTeachPlan(Long companyId, CourseBase courseBase) {
        Long courseId = courseBase.getId();
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengException.cast("非本机构无法修改本机构数据");
        }
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId);
        List<Teachplan> teachPlans = teachplanMapper.selectList(queryWrapper);
        List<Long> teachPlanIds = new ArrayList<>();
        List<Long> teachPlanMediaIds = new ArrayList<>();
        if (teachPlans.size() > 0) {
            for (Teachplan teachPlan : teachPlans) {
                Integer grade = teachPlan.getGrade();
                Long teachPlanId = teachPlan.getId();
                if (grade == 2) {
                    //根据课程计划id获取到需要删除的媒体信息id
                    TeachplanMedia teachplanMedia = teachplanMediaMapper.selectByTeachPlanId(teachPlanId);
                    if (null != teachplanMedia) {
                        Long mediaId = teachplanMedia.getId();
                        teachPlanMediaIds.add(mediaId);
                    }
                }
                teachPlanIds.add(teachPlanId);
            }
        }
        //删除数据前先将存储的视频信息删除，且只有子节点拥有视频信息
        if (teachPlanMediaIds.size() > 0) {
            int j = teachplanMediaMapper.deleteBatchIds(teachPlanMediaIds);
            if (j <= 0) {
                XueChengException.cast("删除视频数据失败");
            }
        }
        //无论是章节还是二级结点，都需要删除，不分等级
        if (teachPlanIds.size() > 0) {
            int i = teachplanMapper.deleteBatchIds(teachPlanIds);
            if (i <= 0) {
                XueChengException.cast("删除章节数据失败");
            }
        }
    }

    /**
     * 上移和下移 简单来讲就是交换顺序 修改order变量
     *
     * @param type 移动类型
     * @param id   课程id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(String type, Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        Long parentId = teachplan.getParentid();
        Integer oldOrder = teachplan.getOrderby();
        Long courseId = teachplan.getCourseId();
        Integer grade = teachplan.getGrade();
        Teachplan nearPlan = getNearPlan(type, parentId, courseId, grade, oldOrder);
        teachplan.setOrderby(nearPlan.getOrderby());
        nearPlan.setOrderby(oldOrder);
        teachplanMapper.updateById(teachplan);
        teachplanMapper.updateById(nearPlan);
    }

    private Teachplan getNearPlan(String type, Long parentId, Long courseId, Integer grade, Integer oldOrder) {
        if (StringUtils.equals(type, "moveup")) {
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getCourseId, courseId)
                    .eq(Teachplan::getGrade, grade)
                    .eq(Teachplan::getParentid, parentId)
                    .lt(Teachplan::getOrderby, oldOrder)
                    .orderByDesc(Teachplan::getOrderby);
            List<Teachplan> teachPlans = teachplanMapper.selectList(queryWrapper);
            if (teachPlans.size() > 0) {
                return teachPlans.get(0);
            } else {
                XueChengException.cast("已经是最上面了，无法上移");
            }
        } else if (StringUtils.equals(type, "movedown")) {
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getCourseId, courseId)
                    .eq(Teachplan::getGrade, grade)
                    .eq(Teachplan::getParentid, parentId)
                    .gt(Teachplan::getOrderby, oldOrder)
                    .orderByAsc(Teachplan::getOrderby);
            List<Teachplan> teachPlans = teachplanMapper.selectList(queryWrapper);
            if (teachPlans.size() > 0) {
                return teachPlans.get(0);
            } else {
                XueChengException.cast("已经是最下面了，无法下移");
            }
        }
        return null;
    }


    /**
     * 获取最新排序号
     *
     * @param courseId 课程id
     * @param parentid 父课程计划id
     * @return
     */
    private int getTeachPlanCount(Long courseId, Long parentid) {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId)
                .eq(Teachplan::getParentid, parentid);
        return teachplanMapper.selectCount(queryWrapper);
    }
}
