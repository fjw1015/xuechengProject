package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author fjw
 * @date 2023/3/15 21:00
 */
@Data
public class CourseCategoryTreeDto extends CourseCategory implements Serializable {
    /**
     * 子节点
     */
    List<CourseCategoryTreeDto> childrenTreeNodes;
}
