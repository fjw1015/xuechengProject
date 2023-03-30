package com.xuecheng.content;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author fjw
 * @date 2023/3/14 23:48
 */
@SpringBootTest
public class CourseBaseInfoServiceTests {
    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @Test
    public void testCourseBaseMapper() {
        PageParams pageParams = new PageParams();
        QueryCourseParamsDto courseParamsDto = new QueryCourseParamsDto();
        courseParamsDto.setCourseName("java");  //课程名称查询条件
        courseParamsDto.setAuditStatus("202004");   //审核通过
        courseParamsDto.setPublishStatus("202004");
        pageParams.setPageNo(1L);
        pageParams.setPageSize(2L);
        System.out.println(courseBaseInfoService.queryCourseBaseList(null, pageParams, courseParamsDto));
    }
}
