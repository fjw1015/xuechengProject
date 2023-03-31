package com.xuecheng.orders.service;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcPayRecord;

/**
 * @author fjw
 * @date 2023/3/31 22:06
 * @description 订单接口
 */
public interface OrderService {
    /**
     * @param addOrderDto 订单信息
     * @return PayRecordDto 支付记录(包括二维码)
     * @description 创建商品订单
     */
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);

    /**
     * @param payNo 交易记录号
     * @description 查询支付记录
     */
    public XcPayRecord getPayRecordByPayno(String payNo);

    /* 请求支付宝查询支付结果
     * @param payNo 支付记录id
     * @return 支付记录信息
     */
    public PayRecordDto queryPayResult(String payNo);

    /**
     * @param payStatusDto 支付结果信息
     * @description 保存支付宝支付结果
     */
    public void saveAliPayStatus(PayStatusDto payStatusDto);

    /**
     * 发送通知结果
     *
     * @param message
     */
    public void notifyPayResult(MqMessage message);
}
