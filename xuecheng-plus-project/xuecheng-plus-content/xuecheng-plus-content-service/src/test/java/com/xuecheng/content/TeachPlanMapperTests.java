package com.xuecheng.content;

import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @author fjw
 * @date 2023/3/14 23:48
 */
@SpringBootTest
public class TeachPlanMapperTests {
    @Autowired
    private TeachplanMapper teachplanMapper;

    @Test
    public void test() {
        List<TeachPlanDto> treeNodes = teachplanMapper.getTreeNodes(117L);
        System.out.println(treeNodes);
    }
}
