package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

/**
 * @author zr
 * 支付宝支付接口
 * @create 2020-04-01 下午 16:42
 */
public interface AlipayService {

    /**
     * 根据订单Id支付
     * 返回String是因为要将二维码显示到浏览器上！ @ResponseBody
     * @param orderId
     * @return
     * @throws AlipayApiException
     */
    String careteAliPay(Long orderId) throws AlipayApiException;

    /**
     * 退款接口
     * @param orderId
     * @return
     */
    boolean refund(Long orderId);

    /**
     * 关闭交易
     * @param orderId
     * @return
     */
    boolean closePay(Long orderId);

    /**
     * 查看是否有交易记录
     * @param orderId
     * @return
     */
    boolean checkPayment(Long orderId);
}
