package com.xuecheng.learning.service;

import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;

/**
 * @author fjw
 * @version 1.0
 * @description 我的课程表service接口
 */
public interface MyCourseTableService {
    /**
     * @param userId   用户id
     * @param courseId 课程id
     * @description 添加选课
     */
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    /**
     * @return XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     * @description 判断学习资格
     */
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId);

    /**
     * 保存选课成功状态
     *
     * @param choosecourseId
     * @return
     */
    boolean saveChooseCourseSuccess(String choosecourseId);

    /**
     * @param params
     * @description 我的课程表
     */
    public PageResult<XcCourseTables> myCourseTables(MyCourseTableParams params);
}
