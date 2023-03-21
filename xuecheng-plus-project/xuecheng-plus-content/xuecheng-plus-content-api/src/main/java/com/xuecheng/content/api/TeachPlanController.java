package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseTeacherDto;
import com.xuecheng.content.model.dto.SaveTeachPlanDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author fjw
 * @date 2023/3/16 17:57
 * @description 课程计划
 */
@RestController
@Api("课程计划接口")
public class TeachPlanController {
    @Autowired
    private TeachplanService teachplanService;

    /**
     * 查询课程计划
     */
    @ApiOperation("课程计划结点信息")
    @ApiImplicitParam(value = "courseId", name = "课程Id", required = true, dataType = "Long", paramType = "path")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachPlanDto> getTreeNodes(@PathVariable Long courseId) {
        return teachplanService.getTreeNodes(courseId);
    }

    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachPlan(@RequestBody SaveTeachPlanDto saveTeachPlanDto) {
        teachplanService.saveTeachPlan(saveTeachPlanDto);
    }

    @ApiOperation("课程计划数据删除")
    @DeleteMapping("/teachplan/{id}")
    public void deleteTeachPlan(@PathVariable Long id) {
        teachplanService.deleteTeachPlan(id);
    }

    @ApiOperation("数据上移")
    @PostMapping("/teachplan/moveup/{id}")
    public void moveUp(@PathVariable Long id) {
        teachplanService.move("moveup",id);
    }

    @ApiOperation("数据下移")
    @PostMapping("/teachplan/movedown/{id}")
    public void moveDown(@PathVariable Long id) {
        teachplanService.move("movedown",id);
    }

}
