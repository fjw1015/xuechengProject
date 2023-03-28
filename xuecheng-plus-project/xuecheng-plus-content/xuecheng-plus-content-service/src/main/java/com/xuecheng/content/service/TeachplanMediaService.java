package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.po.TeachplanMedia;

/**
 * <p>
 * 服务类
 * </p>
 * @author fjw
 * @since 2023-03-14
 */
public interface TeachplanMediaService extends IService<TeachplanMedia> {
    /**
     * @description 教学计划绑定媒资
     */
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);
    
    void deleteMedia(String mediaId, String teachPlanId);
}
