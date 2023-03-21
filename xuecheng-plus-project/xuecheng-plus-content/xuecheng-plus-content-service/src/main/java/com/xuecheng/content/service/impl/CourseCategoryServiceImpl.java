package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 课程分类 服务实现类
 * </p>
 *
 * @author fjw
 */
@Slf4j
@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory> implements CourseCategoryService {
    @Autowired
    private CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //调用mapper查询分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        //找到每个结点的子节点，并封装好
        //先将list转为Map key为结点的id value为Dto对象 filter将根节点排除
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream()
                .filter(item -> !id.equals(item.getId())).collect(Collectors.toMap(
                        key -> key.getId(), value -> value, (key1, key2) -> key2));
        //返回的数据结果
        List<CourseCategoryTreeDto> resultList = new ArrayList<>();
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).forEach(
                item -> {
                    if (item.getParentid().equals(id)) {
                        resultList.add(item);
                    }
                    //子节点 放在父节点childrenTreeNodes中
                    //找到结点的父节点
                    CourseCategoryTreeDto courseCategoryParent = mapTemp.get(item.getParentid());
                    if (null != courseCategoryParent) {
                        if (null == courseCategoryParent.getChildrenTreeNodes()) {
                            //父结点的子为空 New一个集合 向集合中放入它的子节点
                            courseCategoryParent.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                        }
                        //到每个节点的子节点放在父节点的childrenTreeNodes属性中
                        courseCategoryParent.getChildrenTreeNodes().add(item);
                    }
                }
        );
        return resultList;
    }
}
