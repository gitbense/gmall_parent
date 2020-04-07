package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * @author zr
 * @create 2020-04-01 上午 10:00
 */
public interface PaymentService {

    /**
     * 保存交易记录
     * @param orderInfo
     * @param paymentType 支付类型（1：微信 2：支付宝）
     */
    void savePaymentInfo(OrderInfo orderInfo,String paymentType);

    /**
     * 根据第三方交易编号，支付方式查询交易记录对象
     * @param out_trade_no
     * @param paymentType 支付类型（1：微信 2：支付宝）
     * @return
     */
    PaymentInfo getPaymentInfo(String out_trade_no, String paymentType);

    /**
     * 支付成功后，修改交易记录状态
     * @param out_trade_no
     * @param paymentType 支付类型（1：微信 2：支付宝）
     * @param paramMap
     */
    void paySuccess(String out_trade_no, String paymentType, Map<String, String> paramMap);

    /**
     * 更新交易记录
     * @param out_trade_no
     * @param paymentInfo
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfo);

    /**
     * 关闭过期交易记录
     * @param orderId
     */
    void closePayment(Long orderId);
}
