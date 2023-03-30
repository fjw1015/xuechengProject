package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;

/**
 * @author fjw
 * @version 1.0
 * @description 认证service
 */
public interface AuthService {

    /**
     * @param authParamsDto 认证参数
     * @return 用户信息
     * @description 认证方法
     */
    XcUserExt execute(AuthParamsDto authParamsDto);

}
