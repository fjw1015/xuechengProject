package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.po.XcUser;

/**
 * @author fjw
 * @date 2023/3/30 20:34
 * @description
 */
public interface WxAuthService {
    public XcUser wxAuth(String code);
}
