package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * @author zr
 * @create 2020-04-01 下午 12:20
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private AlipayService alipayService;

    // 保存交易记录
    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id", orderInfo.getId()).eq("payment_type", paymentType);
        Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        if (count > 0) return;

        // 保存交易记录
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());

        paymentInfoMapper.insert(paymentInfo);
    }

    // 根据第三方交易编号，支付方式查询交易记录对象
    @Override
    public PaymentInfo getPaymentInfo(String out_trade_no, String paymentType) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", out_trade_no).eq("payment_type", paymentType);

        return paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
    }

    // 支付成功后，修改交易记录状态
    @Override
    public void paySuccess(String out_trade_no, String paymentType, Map<String, String> paramMap) {
        // update payment_info set payment_status = PAID,callback_time = new Date(),callback_content = paramMap where out_trade_no = out_trade_no and payment_type = ALIPAY
        PaymentInfo info = getPaymentInfo(out_trade_no, paymentType);
        if (info.getPaymentStatus() == PaymentStatus.PAID.name() || info.getPaymentStatus() == PaymentStatus.ClOSED.name()) {
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(paramMap.toString());
        // 获取支付宝的交易编号
        paymentInfo.setTradeNo(paramMap.get("trade_no"));

        // 调用更新方法
        updatePaymentInfo(out_trade_no, paymentInfo);

        // 后续更新订单状态，使用消息队列
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY, paymentInfo.getOrderId());
    }

    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfo) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", out_trade_no);
        paymentInfoMapper.update(paymentInfo, paymentInfoQueryWrapper);
    }

    // 关闭过期交易记录
    @Override
    public void closePayment(Long orderId) {
        // 设置关闭交易记录的条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id", orderId);
        // 判断当前交易记录是否存在，不存在则不更新交易记录
        Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        if (null == count || count.intValue() == 0) return;
        // 关闭支付宝交易之前，还需要关闭paymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());

        paymentInfoMapper.update(paymentInfo, paymentInfoQueryWrapper);

        // 关闭支付宝中交易
        alipayService.closePay(orderId);
    }
}
