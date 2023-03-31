package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTableService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author fjw
 * @version 1.0
 * @description 接收支付结果
 */
@Slf4j
@Service
public class ReceivePayNotifyService {
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    MqMessageService mqMessageService;
    @Autowired
    MyCourseTableService myCourseTableService;

    //监听消息队列接收支付结果通知
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] body = message.getBody();
        String jsonString = new String(body);
        //获取消息 转为对象
        MqMessage mqMessage = JSON.parseObject(jsonString, MqMessage.class);
        log.info("学习中心服务接收支付结果:{}", mqMessage);
        //消息类型
        String messageType = mqMessage.getMessageType();
        //订单类型 , 60201表示购买课程
        String orderType = mqMessage.getBusinessKey2();
        //学习中心服务只要购买课程类的支付订单的结果
        //这里只处理支付结果通知
        if (PayNotifyConfig.MESSAGE_TYPE.equals(messageType) && "60201".equals(orderType)) {
            //选课记录id
            String choosecourseId = mqMessage.getBusinessKey1();
            //添加选课
            boolean b = myCourseTableService.saveChooseCourseSuccess(choosecourseId);
            if (!b) {
                //添加选课失败，抛出异常，消息重回队列
                XueChengException.cast("收到支付结果，添加选课失败");
            }
        }
    }
}