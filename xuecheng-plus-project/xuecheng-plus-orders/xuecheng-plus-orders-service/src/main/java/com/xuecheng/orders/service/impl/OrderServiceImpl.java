package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author fjw
 * @date 2023/3/31 22:06
 * @description
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    XcOrdersMapper ordersMapper;
    @Autowired
    XcOrdersGoodsMapper ordersGoodsMapper;
    @Value("${pay.qrcodeurl}")
    String qrcodeurl;
    @Autowired
    XcPayRecordMapper payRecordMapper;
    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;
    @Autowired
    OrderServiceImpl currentProxy;
    @Autowired
    MqMessageService mqMessageService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        //创建商品订单
        XcOrders orders = saveXcOrders(userId, addOrderDto);
        if (orders == null) {
            XueChengException.cast("订单创建失败");
        }
        //生成支付记录
        XcPayRecord payRecord = createPayRecord(orders);
        //生成二维码
        String qrCode = null;
        try {
            //url要可以被模拟器访问到，url为下单接口(稍后定义)
            String url = String.format(qrcodeurl, payRecord.getPayNo());
            qrCode = new QRCodeUtil().createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengException.cast("生成二维码出错");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        //二维码图片
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        //幂等性处理
        XcOrders order = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (order != null) {
            return order;
        }
        order = new XcOrders();
        //生成订单号 使用雪花算法
        long orderId = IdWorkerUtils.getInstance().nextId();
        order.setId(orderId);
        order.setTotalPrice(addOrderDto.getTotalPrice());
        order.setCreateDate(LocalDateTime.now());
        order.setStatus("600001"); //未支付
        order.setUserId(userId);
        order.setOrderType(addOrderDto.getOrderType());
        order.setOrderName(addOrderDto.getOrderName());
        order.setOrderDetail(addOrderDto.getOrderDetail());
        order.setOrderDescrip(addOrderDto.getOrderDescrip());
        order.setOutBusinessId(addOrderDto.getOutBusinessId());//选课记录id
        int insert = ordersMapper.insert(order);
        if (insert <= 0) {
            XueChengException.cast("添加订单失败");
        }
        //插入订单明细表
        String orderDetailJson = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoodsList = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        xcOrdersGoodsList.forEach(goods -> {
            XcOrdersGoods xcOrdersGoods = new XcOrdersGoods();
            BeanUtils.copyProperties(goods, xcOrdersGoods);
            xcOrdersGoods.setOrderId(orderId);//订单号
            int i = ordersGoodsMapper.insert(xcOrdersGoods);
            if (i <= 0) {
                XueChengException.cast("插入订单明细数据失败");
            }
        });
        return order;
    }

    //根据业务id查询订单
    public XcOrders getOrderByBusinessId(String businessId) {
        LambdaQueryWrapper<XcOrders> queryWrapper = new LambdaQueryWrapper<XcOrders>()
                .eq(XcOrders::getOutBusinessId, businessId);
        return ordersMapper.selectOne(queryWrapper);
    }

    public XcPayRecord createPayRecord(XcOrders orders) {
        //如果订单不存在 不能添加支付记录
        Long id = orders.getId();
        XcOrders order = ordersMapper.selectById(id);
        if (order == null) {
            XueChengException.cast("订单不存在");
        }
        String status = orders.getStatus();
        if ("600002".equals(status)) {
            XueChengException.cast("订单已支付");
        }
        XcPayRecord payRecord = new XcPayRecord();
        //生成支付交易流水号
        long payNo = IdWorkerUtils.getInstance().nextId();
        payRecord.setPayNo(payNo);
        payRecord.setOrderId(orders.getId());//商品订单号
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");  //未支付
        payRecord.setUserId(orders.getUserId());
        int insert = payRecordMapper.insert(payRecord);
        if (insert <= 0) {
            XueChengException.cast("支付记录添加失败");
        }
        return payRecord;
    }

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        LambdaQueryWrapper<XcPayRecord> queryWrapper = new LambdaQueryWrapper<XcPayRecord>()
                .eq(XcPayRecord::getPayNo, payNo);
        return payRecordMapper.selectOne(queryWrapper);
    }

    @Override
    public PayRecordDto queryPayResult(String payNo) {
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XueChengException.cast("请重新点击支付获取二维码");
        }
        //支付状态
        String status = payRecord.getStatus();
        //如果支付成功直接返回
        if ("601002".equals(status)) {
            PayRecordDto payRecordDto = new PayRecordDto();
            BeanUtils.copyProperties(payRecord, payRecordDto);
            return payRecordDto;
        }
        //从支付宝查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        //保存支付结果
        currentProxy.saveAliPayStatus(payStatusDto);
        //重新查询支付记录
        payRecord = getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        return payRecordDto;
    }

    /**
     * 请求支付宝查询支付结果
     *
     * @param payNo 支付交易号
     * @return 支付结果
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo) {
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        String body = null;
        try {
            response = alipayClient.execute(request);
            //交易不成功
            if (!response.isSuccess()) {
                XueChengException.cast("请求支付查询支付结果失败");
            }
            //获取支付结果
            body = response.getBody();
        } catch (AlipayApiException e) {
            log.error("请求支付宝查询支付结果异常:{}", e.toString(), e);
            XueChengException.cast("请求支付查询支付结果失败");
        }
        //转map
        Map resultMap = JSON.parseObject(body, Map.class);
        Map alipayTradeQueryResponse = (Map) resultMap.get("alipay_trade_query_response");
        //支付结果
        String tradeStatus = (String) alipayTradeQueryResponse.get("trade_status");
        String totalAmount = (String) alipayTradeQueryResponse.get("total_amount");
        String tradeNo = (String) alipayTradeQueryResponse.get("trade_no");
        //保存支付结果
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_status(tradeStatus);
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTrade_no(tradeNo);
        payStatusDto.setTotal_amount(totalAmount);
        return payStatusDto;
    }

    @Transactional
    @Override
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        //支付流水号
        String payNo = payStatusDto.getOut_trade_no();
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XueChengException.cast("支付记录找不到");
        }
        //支付结果
        String tradeStatus = payStatusDto.getTrade_status();
        log.info("收到支付结果:{},支付记录:{}}", payStatusDto, payRecord);
        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            //支付金额变为分
            float totalPrice = payRecord.getTotalPrice() * 100;
            float totalAmount = Float.parseFloat(payStatusDto.getTotal_amount()) * 100;
            //校验是否一致
            if ((int) totalPrice != (int) totalAmount) {
                //校验失败
                log.info("校验支付结果失败,支付记录:{},APP_ID:{},totalPrice:{}", payRecord.toString(), payStatusDto.getApp_id(), (int) totalAmount);
                XueChengException.cast("校验支付结果失败");
            }
            log.info("更新支付结果,支付交易流水号:{},支付结果:{}", payNo, tradeStatus);
            XcPayRecord xcPayRecord = new XcPayRecord();
            //支付成功
            xcPayRecord.setStatus("601002");
            //第三方支付渠道
            xcPayRecord.setOutPayChannel("Alipay");
            //支付宝交易号 订单号
            xcPayRecord.setOutPayNo(payStatusDto.getTrade_no());
            //通知时间
            xcPayRecord.setPaySuccessTime(LocalDateTime.now());
            LambdaQueryWrapper<XcPayRecord> queryWrapper = new LambdaQueryWrapper<XcPayRecord>()
                    .eq(XcPayRecord::getPayNo, payNo);
            int i = payRecordMapper.update(xcPayRecord, queryWrapper);
            if (i >= 0) {
                log.info("更新支付记录状态成功:{}", xcPayRecord.toString());
            } else {
                log.info("更新支付记录状态失败:{}", xcPayRecord.toString());
                XueChengException.cast("更新支付记录状态失败");
            }
            //关联的订单号
            Long orderId = payRecord.getOrderId();
            XcOrders orders = ordersMapper.selectById(orderId);
            if (orders == null) {
                log.info("根据支付记录[{}}]找不到订单", xcPayRecord.toString());
                XueChengException.cast("根据支付记录找不到订单");
            }
            XcOrders xcOrders = new XcOrders();
            xcOrders.setStatus("600002");//支付成功
            int update = ordersMapper.update(xcOrders, new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getId, orderId));
            if (update > 0) {
                log.info("更新订单表状态成功,订单号:{}", orderId);
            } else {
                log.info("更新订单表状态失败,订单号:{}", orderId);
                XueChengException.cast("更新订单表状态失败");
            }
            //将消息写入数据库,参数1：支付结果通知类型，2: 业务id，3:业务类型
            MqMessage mqMessage = mqMessageService.addMessage("payresult_notify", orders.getOutBusinessId(), orders.getOrderType(), null);
            //通知消息
            notifyPayResult(mqMessage);
        }
    }

    @Override
    public void notifyPayResult(MqMessage message) {
        //消息体，转json
        String msg = JSON.toJSONString(message);
        //设置消息持久化
        Message msgObj = MessageBuilder.withBody(msg.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        // 全局唯一的消息ID，需要封装到CorrelationData中
        CorrelationData correlationData = new CorrelationData(message.getId().toString());
        // 添加callback
        correlationData.getFuture().addCallback(result -> {
                    if (null != result) {
                        if (result.isAck()) {
                            // ack，消息成功
                            log.debug("通知支付结果消息发送成功, ID:{}", correlationData.getId());
                            //将消息从数据库表mq_message中删除
                            mqMessageService.completed(message.getId());
                        } else {
                            // nack，消息失败
                            log.error("通知支付结果消息发送失败, ID:{}, 原因{}", correlationData.getId(), result.getReason());
                        }
                    }
                },
                ex -> {
                    //发生异常
                    log.error("消息发送异常, ID:{}, 原因{}", correlationData.getId(), ex.getMessage());
                }
        );
        // 发送消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT, "", msgObj, correlationData);
    }
}
