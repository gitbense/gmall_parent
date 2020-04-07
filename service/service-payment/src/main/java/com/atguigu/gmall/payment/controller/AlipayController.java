package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * @author zr
 * @create 2020-04-01 下午 17:00
 */
@Controller
@RequestMapping("api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    // 配置对应提交过来的映射
    @RequestMapping("submit/{orderId}")
    @ResponseBody // 底层 jackson.jar
    public String submitOrder(@PathVariable Long orderId) {
        String from = "";
        try {
            // 返回生成的二维码字符串
            from = alipayService.careteAliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        return from;
    }

    // http://api.gmall.com/api/payment/alipay/callback/return
    @RequestMapping("callback/return")
    public String callBack() {
        // 同步回调给用户展示信息
        // return_order_url=http://payment.gmall.com/pay/success.html
        return "redirect:" + AlipayConfig.return_order_url;
    }

    @RequestMapping("callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String, String> paramMap) {
        // Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = false;
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        // 获取交易状态
        String trade_status = paramMap.get("trade_status");
        // 获取订单号
        String out_trade_no = paramMap.get("out_trade_no");
        if (signVerified) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
                // 更改交易记录的状态
                // 在更改交易记录状态时，先判断当前状态，如果为PAID或CLOSED，则不能更新状态
                // select * from payment_info where out_trade_no = out_trade_no and payment_type = ALIPAY
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(out_trade_no, PaymentType.ALIPAY.name());
                // 判断业务
                if (paymentInfo.getPaymentStatus() == PaymentStatus.PAID.name() || paymentInfo.getPaymentStatus() == PaymentStatus.ClOSED.name()) {
                    return "failure";
                }
                // 正常支付成功，更改交易记录状态
                // update payment_info set payment_status = PAID,callback_content = paramMap where out_trade_no = out_trade_no and payment_type = ALIPAY
                paymentService.paySuccess(out_trade_no, PaymentType.ALIPAY.name(), paramMap);
                return "success";
            }
        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    // 发起退款！http://localhost:8205/api/payment/alipay/refund/20
    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId) {
        // 调用退款接口
        boolean flag = alipayService.refund(orderId);

        return Result.ok(flag);
    }

    // 查看是否有交易记录
    @RequestMapping("checkPayment/{orderId}")
    @ResponseBody
    public boolean checkPayment(@PathVariable Long orderId) {
        // 调用接口
        return alipayService.checkPayment(orderId);
    }
}
