package com.xuecheng.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.content.model.po.TeachplanMedia;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author fjw
 */
public interface TeachplanMediaMapper extends BaseMapper<TeachplanMedia> {
    int deleteByTeachplanId(Long teachPlanId);

    TeachplanMedia selectByTeachPlanId(Long teachPlanId);
}
