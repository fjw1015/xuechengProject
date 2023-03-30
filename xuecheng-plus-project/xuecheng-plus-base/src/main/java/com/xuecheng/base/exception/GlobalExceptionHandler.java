package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fjw
 * @date 2023/3/16 0:12
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 对项目自定义异常处理
     *
     * @param e
     * @return
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(XueChengException.class)
    public RestErrorResponse customException(XueChengException e) {
        log.error("系统自定义异常 {}", e.getErrMessage(), e);
        //解析异常信息
        String errMessage = e.getErrMessage();
        return new RestErrorResponse(errMessage);
    }

    /**
     * 对项目自定义异常处理
     *
     * @param e
     * @return
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public RestErrorResponse exception(Exception e) {
        log.error("系统默认异常 {}", e.getMessage(), e);
        if (e.getMessage().equals("不允许访问")) {
            return new RestErrorResponse("您没有权限操作此功能");
        }
        //解析异常信息
        return new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RestErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<String> errors = new ArrayList<>();
        bindingResult.getFieldErrors().stream().forEach(item -> {
            errors.add(item.getDefaultMessage());
        });
        //将错误信息拼接起来
        String msg = StringUtils.join(errors, ",");
        log.error("系统处理异常 {}", msg);
        //解析异常信息
        return new RestErrorResponse(msg);
    }

}
