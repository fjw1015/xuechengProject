package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;

import java.util.List;

/**
 * @author fjw
 * @date 2023/3/16 17:54
 * @description 课程计划信息
 */
@Data
public class TeachPlanDto extends Teachplan {
    //小章节列表
    private List<TeachPlanDto> teachPlanTreeNodes;
    private TeachplanMedia teachplanMedia;

}
