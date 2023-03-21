package com.xuecheng.base.exception;

/**
 * @author fjw
 * @date 2023/3/16 0:08
 * @description 本项目自定义异常类型
 */
public class XueChengException extends RuntimeException {
    private String errMessage;

    public XueChengException() {

    }

    public XueChengException(String message) {
        super(message);
        this.errMessage = message;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    public static void cast(String errMessage) {
        throw new XueChengException(errMessage);
    }

    public static void cast(CommonError error) {
        throw new XueChengException(error.getErrMessage());
    }
}
