package com.xuecheng.base.exception;

/**
 * @author fjw
 * @date 2023/3/16 0:06
 * @description 异常类
 */
public class RestErrorResponse {
    private String errMessage;

    public RestErrorResponse(String errMessage) {
        this.errMessage = errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }
}
